package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.dto.response.PublicMenuResponse;
import com.example.restaurant.helpers.PagedResult;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public interface IDishServices {
    PagedResult<DishListResponse> getMenu(DishFilterRequest request, PaggedRequest pagged);

    void remove(String dishToken);

    void changeAvailable(ChangeDishAvailableRequest request);

    void edit(EditDishRequest request);

    void add(AddDishRequest request);

    DictionaryResponse getDictionary();

    void addCategory(AddEntityRequest request);

    void removeCategory(String token);

    PublicMenuResponse getPublicMenu();
}
