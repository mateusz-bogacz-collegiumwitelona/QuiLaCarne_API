package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.models.Dishes;

import java.util.List;

public interface IDishRepository {
    PagedResult<DishListResponse> findAllDishes(String lang, DishFilterRequest request, PaggedRequest pagged);

    List<Dishes> listForOrder(List<String> tokens);

    Dishes get(List<Dishes> dishes, String token);
}
