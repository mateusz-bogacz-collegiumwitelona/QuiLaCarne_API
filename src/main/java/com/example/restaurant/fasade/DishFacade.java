package com.example.restaurant.fasade;

import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.*;
import com.example.restaurant.fasade.interfaces.IDishFacade;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.services.dish.DishCatalogService;
import com.example.restaurant.services.dish.DishCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("java:S2166")
public class DishFacade implements IDishFacade {
  private final DishCatalogService _catalogService;
  private final DishCategoryService _categoryService;

  @Override
  public PagedResult<DishListResponse> getMenu(DishFilterRequest request, PaggedRequest pagged) {
    return _catalogService.getMenu(request, pagged);
  }

  @Override
  public void remove(String dishToken) {
    _catalogService.remove(dishToken);
  }

  @Override
  public void changeAvailable(ChangeDishAvailableRequest request) {
    _catalogService.changeAvailable(request);
  }

  @Override
  public void edit(EditDishRequest request) {
    _catalogService.edit(request);
  }

  @Override
  public void add(AddDishRequest request) {
    _catalogService.add(request);
  }

  @Override
  public DictionaryResponse getDictionary() {
    return _categoryService.getDictionary();
  }

  @Override
  public void addCategory(AddEntityRequest request) {
    _categoryService.addCategory(request);
  }

  @Override
  public void removeCategory(String token) {
    _categoryService.removeCategory(token);
  }

  @Override
  public PublicMenuResponse getPublicMenu() {
    return _catalogService.getPublicMenu();
  }
}
