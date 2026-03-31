package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddIngredientRequest;

public interface IIngredientsServices {
    void add(AddIngredientRequest request);

    void remove(String token);
}
