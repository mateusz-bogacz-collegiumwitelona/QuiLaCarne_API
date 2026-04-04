package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.helpers.PagedResult;

import java.util.List;

public interface IDishServices {
    PagedResult<DishListResponse> getMenu(DishFilterRequest request, PaggedRequest pagged);

    void remove(String dishToken);

    void changeAvailable(ChangeDishAvailableRequest request);

    void edit(EditDishRequest request);

    void add(AddDishRequest request);

    List<EntityResponse> getDictionary();

    void addCategory(AddEntityRequest request);

    void removeCategory(String token);
}
