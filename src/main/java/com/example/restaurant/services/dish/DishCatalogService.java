package com.example.restaurant.services.dish;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.dto.response.DishResponse;
import com.example.restaurant.dto.response.MenuResponse;
import com.example.restaurant.dto.response.PublicMenuResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IStorageService;
import com.example.restaurant.validators.dish.DishSearchStrategy;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishCatalogService {
  private final IDishRepository _dishRepo;
  private final DishMapper _dishMapper;
  private final IStorageService _s3Services;
  private final IIngredientsRepository _ingredientsRepo;
  private final DishMediaService _mediaService;
  private final DishSyncPublisher _syncPublisher;

  private final List<DishSearchStrategy> _searchStrategies;

  @Cacheable(
      value = "dishMenu",
      key =
          "#request.toString() + '-' + #pagged.toString() + '-' + "
              + "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public PagedResult<DishListResponse> getMenu(DishFilterRequest request, PaggedRequest pagged) {
    String lang = LocaleContextHolder.getLocale().getLanguage();

    int pageIndex = Math.max(0, pagged.getPage() - 1);
    Pageable pageable = PageRequest.of(pageIndex, pagged.getSize());

    Page<Dishes> dishPage;

    dishPage =
        _searchStrategies.stream()
            .filter(s -> s.supports(request))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No strategy for dish search"))
            .find(request, pageable);

    Page<DishListResponse> result =
        dishPage.map(
            d -> {
              DishListResponse dto = _dishMapper.toDishListResponse(d, lang);

              dto.setImageUrl(_mediaService.getFullImageUrl(dto.getImageUrl()));

              return dto;
            });

    return new PagedResult<>(result);
  }

  @Transactional
  @Auditable(action = "REMOVE_DISH")
  @Caching(
      evict = {
        @CacheEvict(value = "dishMenu", allEntries = true),
        @CacheEvict(value = "publicDishMenu", allEntries = true)
      })
  public void remove(String dishToken) {
    Dishes dish = _dishRepo.findByToken(dishToken);

    dish.setUnavailableReason("Dish is deleted");
    dish.setAvailable(false);
    dish.setDeletedAt(OffsetDateTime.now());

    _s3Services.deleteFile(dish.getImageUrl());
    dish.setImageUrl(null);

    _dishRepo.save(dish);

    _syncPublisher.publishDishDeleted(dishToken);
  }

  @Transactional
  @Auditable(action = "CHANGE_DISH_AVAILABLE")
  @Caching(
      evict = {
        @CacheEvict(value = "dishMenu", allEntries = true),
        @CacheEvict(value = "publicDishMenu", allEntries = true)
      })
  public void changeAvailable(ChangeDishAvailableRequest request) {
    Dishes dish = _dishRepo.findByToken(request.getToken());

    dish.setAvailable(request.isAvailable());

    if (request.isAvailable()) {
      dish.setUnavailableReason(null);
    } else {
      String reason = request.getUnavailableReason();
      dish.setUnavailableReason(
          reason != null && !reason.isBlank() ? reason.trim() : "Brak składników");
    }

    _dishRepo.save(dish);

    _syncPublisher.publishDishUpdated(dish);
  }

  @Transactional
  @Auditable(action = "ADD_DISH")
  @CacheEvict(value = "dishMenu", allEntries = true)
  public void add(AddDishRequest request) {
    Dishes dish = new Dishes();

    dish.setName(request.getName().trim());
    dish.setPrice(request.getPrice());

    DishesCategories category = _dishRepo.findCategoryByToken(request.getCategoryToken());
    dish.setCategory(category);

    dish.setAvailable(true);
    updateDishIngredients(dish, request.getIngredientTokens());
    _mediaService.updateDishPhoto(dish, request.getPhoto());

    _dishRepo.save(dish);

    _syncPublisher.publishDishCreated(dish);
  }

  @Transactional
  @Auditable(action = "EDIT_DISH")
  @Caching(
      evict = {
        @CacheEvict(value = "dishMenu", allEntries = true),
        @CacheEvict(value = "publicDishMenu", allEntries = true)
      })
  public void edit(EditDishRequest request) {
    Dishes dish = _dishRepo.findByToken(request.getDishToken());

    if (request.getNewName() != null && !request.getNewName().isBlank())
      dish.setName(request.getNewName().trim());

    if (request.getPrice() != null) dish.setPrice(request.getPrice());

    if (request.getCategoryToken() != null) {
      DishesCategories category = _dishRepo.findCategoryByToken(request.getCategoryToken());
      dish.setCategory(category);
    }

    updateDishIngredients(dish, request.getIngredientTokens());
    _mediaService.updateDishPhoto(dish, request.getPhoto());

    _dishRepo.save(dish);

    _syncPublisher.publishDishUpdated(dish);
  }

  @Cacheable(
      value = "publicDishMenu",
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public PublicMenuResponse getPublicMenu() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    List<Dishes> allDishes = _dishRepo.findAll();

    List<MenuResponse> menu =
        allDishes.stream()
            .filter(Dishes::isAvailable)
            .collect(Collectors.groupingBy(d -> d.getCategory().translate(lang)))
            .entrySet()
            .stream()
            .map(
                d -> {
                  String category = d.getKey();

                  List<DishResponse> dishesInCategory =
                      d.getValue().stream().map(dish -> mapToDishResponse(dish, lang)).toList();

                  return MenuResponse.builder().category(category).dish(dishesInCategory).build();
                })
            .toList();

    return new PublicMenuResponse(menu);
  }

  private void updateDishIngredients(Dishes dish, List<String> tokens) {
    if (!ObjectUtils.isEmpty(tokens)) {
      Set<Ingredients> newIngredients = new HashSet<>();
      for (String token : tokens) {
        Ingredients ingredient = _ingredientsRepo.findByToken(token);
        newIngredients.add(ingredient);
      }
      dish.setIngredients(newIngredients);
    }
  }

  private DishResponse mapToDishResponse(Dishes dish, String lang) {
    List<String> ingridents = dish.getIngredients().stream().map(i -> i.translate(lang)).toList();

    List<String> allergens =
        dish.getIngredients().stream()
            .flatMap(i -> i.getAllergens().stream())
            .map(a -> a.translate(lang))
            .distinct()
            .toList();

    String imageUrl = _mediaService.getFullImageUrl(dish.getImageUrl());

    return DishResponse.builder()
        .name(dish.getName())
        .price(dish.getPrice())
        .ingridents(ingridents)
        .imageUrl(imageUrl)
        .allergens(allergens)
        .build();
  }
}
