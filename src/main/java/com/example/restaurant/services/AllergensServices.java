package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IAllergensServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllergensServices implements IAllergensServices {
  private final IAllergensRepository _allergenRepo;
  private final IIngredientsRepository _ingredientsRepo;
  private final NotificationServices _notification;

  private final SyncMapper _syncMapper;

  private static final String ENTITY_TYPE = "ALLERGEN";

  @Override
  @Cacheable(
      value = "allergensDictionary",
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public DictionaryResponse getDictionary() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    return new DictionaryResponse(DictionaryHelper.map(_allergenRepo.findAll(), lang));
  }

  @Override
  @Transactional
  @Auditable(action = "ADD_ALLERGEN")
  @CacheEvict(value = "allergensDictionary", allEntries = true)
  public void add(AddEntityRequest request) {
    Allergens allergen =
        DictionaryHelper.createEntity(
            Allergens::new, request, _allergenRepo::isNameTaken, "Allergen already exist");

    _allergenRepo.save(allergen);

    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(allergen);
    WebSocketEvent<SyncDictionaryResponse> event =
        WebSocketEvent.created(ENTITY_TYPE, allergen.getToken(), payload);
    _notification.sendEventToTopic("/dictionary/allergens", event);
    log.info("Added allergens for dictionary: {}", allergen.getToken());
  }

  @Override
  @Transactional
  @Auditable(action = "REMOVE_ALLERGEN")
  @Caching(
      evict = {
        @CacheEvict(value = "allergensDictionary", allEntries = true),
        @CacheEvict(value = "ingredientsDictionary", allEntries = true),
        @CacheEvict(value = "publicDishMenu", allEntries = true),
        @CacheEvict(value = "dishMenu", allEntries = true)
      })
  public void remove(String token) {
    DictionaryHelper.deleteEntity(
        token,
        _allergenRepo::findByToken,
        _allergenRepo::save,
        a -> {
          for (Ingredients ingredient : a.getIngredients()) {
            ingredient.getAllergens().remove(a);
            _ingredientsRepo.save(ingredient);
          }
        });

    WebSocketEvent<Void> event = WebSocketEvent.deleted(ENTITY_TYPE, token);

    _notification.sendEventToTopic("/dictionary/allergens", event);
    log.info("Removed allergens for dictionary: {}", token);
  }
}
