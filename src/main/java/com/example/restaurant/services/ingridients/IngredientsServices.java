package com.example.restaurant.services.ingridients;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IIngredientsServices;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
public class IngredientsServices implements IIngredientsServices {
  private final IIngredientsRepository _ingredientsRepo;
  private final IAllergensRepository _allergensRepo;
  private final IDishRepository _dishRepo;
  private final IngredientSyncPublisher _syncPublisher;

  @Transactional
  @Override
  @Auditable(action = "ADD_INGREDIENTS")
  @CacheEvict(value = "ingredientsDictionary", allEntries = true)
  public void add(AddIngredientRequest request) {
    Ingredients ingredient =
        DictionaryHelper.createEntity(
            Ingredients::new,
            request.getEntity(),
            _ingredientsRepo::isNameTaken,
            "Ingredient already exists");

    List<String> allergenTokens =
        request.getAllergenTokens() != null
            ? new ArrayList<>(request.getAllergenTokens())
            : new ArrayList<>();

    var allergens = _allergensRepo.findAllergens(allergenTokens);

    if (allergenTokens.size() != allergens.size()) {
      throw new IllegalStateException("One or more allergens from the provided list do not exist");
    }

    ingredient.setAllergens(new HashSet<>(allergens));

    _ingredientsRepo.save(ingredient);

    _syncPublisher.publishIngredientsCreate(ingredient);

    log.info("Create new ingrediant {}", ingredient.getToken());
  }

  @Transactional
  @Override
  @Auditable(action = "REMOVE_INGREDIENTS")
  @Caching(
      evict = {
        @CacheEvict(value = "ingredientsDictionary", allEntries = true),
        @CacheEvict(value = "dishMenu", allEntries = true),
        @CacheEvict(value = "publicDishMenu", allEntries = true)
      })
  public void remove(String token) {
    DictionaryHelper.deleteEntity(
        token,
        _ingredientsRepo::findByToken,
        _ingredientsRepo::save,
        i -> {
          String orginaNameEn = i.getNameEn();

          var affectedDishes = _dishRepo.findByIngredientsId(i.getId());

          for (Dishes dish : affectedDishes) {
            dish.setAvailable(false);
            dish.setUnavailableReason(orginaNameEn + " is deleted");
            _dishRepo.save(dish);
          }
        });

    _syncPublisher.publishIngredientsDelete(token);

    log.info("Delete ingrediant {}", token);
  }

  @Override
  @Cacheable(
      value = "ingredientsDictionary",
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public DictionaryResponse getDictionary() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    return new DictionaryResponse(DictionaryHelper.map(_ingredientsRepo.findAll(), lang));
  }
}
