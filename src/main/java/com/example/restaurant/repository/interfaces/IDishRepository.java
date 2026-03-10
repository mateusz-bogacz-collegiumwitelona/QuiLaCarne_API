package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResultWrapper;

import java.util.List;

public interface IDishRepository {
    public PagedResultWrapper<DishListResponse> findAllDishes(String lang, DishFilterRequest request, PaggedRequest pagged);
}
