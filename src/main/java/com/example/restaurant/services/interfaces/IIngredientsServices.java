package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.helpers.ResultHandler;

public interface IIngredientsServices {
    ResultHandler<Void> add(AddIngredientRequest request);

    ResultHandler<Void> remove(String token);
}
