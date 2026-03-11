package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;

public interface IDishRepository {
    PagedResult<DishListResponse> findAllDishes(String lang, DishFilterRequest request, PaggedRequest pagged);
    boolean isDishExist(String token);
}
