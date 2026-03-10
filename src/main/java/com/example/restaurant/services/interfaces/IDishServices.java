package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResultWrapper;
import com.example.restaurant.helpers.ResultHandler;

import java.util.List;

public interface IDishServices {
    public ResultHandler<PagedResultWrapper<DishListResponse>> getMenu(DishFilterRequest request, PaggedRequest pagged);
}
