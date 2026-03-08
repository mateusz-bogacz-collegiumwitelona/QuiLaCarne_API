package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.response.DishListResponse;

import java.util.List;

public interface IDishRepository {
    public List<DishListResponse> findAllDishes(String lang);
}
