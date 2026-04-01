package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.dto.response.EntityResponse;

import java.util.List;

public interface IIngredientsServices {
    void add(AddIngredientRequest request);

    void remove(String token);

    List<EntityResponse> getDictionary();
}
