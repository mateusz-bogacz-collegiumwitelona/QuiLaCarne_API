package com.example.restaurant.services.dish;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.repository.interfaces.IDishRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DishCategoryService {
  private final IDishRepository _dishRepo;
  private final DishSyncPublisher _syncPublisher;

  @Cacheable(
      value = "dishCategories",
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public DictionaryResponse getDictionary() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    return new DictionaryResponse(DictionaryHelper.map(_dishRepo.findAllCategories(), lang));
  }

  @Transactional
  @Auditable(action = "ADD_DISH_CATEGORY")
  @CacheEvict(value = "dishCategories", allEntries = true)
  public void addCategory(AddEntityRequest request) {
    DishesCategories category =
        DictionaryHelper.createEntity(
            DishesCategories::new,
            request,
            _dishRepo::isCategoryNameTaken,
            "Dish category already exists");

    _dishRepo.saveCategory(category);

    _syncPublisher.publishCategoryCreated(category);
  }

  @Transactional
  @Auditable(action = "REMOVE_DISH_CATEGORY")
  @Caching(
      evict = {
        @CacheEvict(value = "dishCategories", allEntries = true),
        @CacheEvict(value = "dishMenu", allEntries = true),
        @CacheEvict(value = "publicDishMenu", allEntries = true)
      })
  public void removeCategory(String token) {
    DictionaryHelper.deleteEntity(
        token,
        _dishRepo::findCategoryByToken,
        _dishRepo::saveCategory,
        c -> {
          DishesCategories fallback = _dishRepo.findCategoryByToken("OTHER");

          List<Dishes> affected = _dishRepo.findByCategoryId(c.getId());

          for (Dishes dish : affected) {
            dish.setCategory(fallback);
            _dishRepo.save(dish);
          }
        });

    _syncPublisher.publishCategoryDeleted(token);
  }
}
