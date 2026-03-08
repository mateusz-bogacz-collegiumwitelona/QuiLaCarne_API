package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.ResultHandler;

import java.util.List;

public interface IDishServices {
    public ResultHandler<List<DishListResponse>> getMenu();
}
