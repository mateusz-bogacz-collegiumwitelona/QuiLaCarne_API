package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;

public interface IDishServices {
    ResultHandler<PagedResult<DishListResponse>> getMenu(DishFilterRequest request, PaggedRequest pagged);
}
