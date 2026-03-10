package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;

import java.util.List;

public interface IDishRepository {
    public List<DishListResponse> findAllDishes(String lang, DishFilterRequest request, PaggedRequest pagged);
}
