package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.helpers.ResultHandler;

public interface IIngredientsServices {
    ResultHandler<Void> add(AddEntityRequest request);
}
