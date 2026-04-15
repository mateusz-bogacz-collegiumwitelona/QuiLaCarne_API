package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.dto.response.DictionaryResponse;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public interface IIngredientsServices {
    void add(AddIngredientRequest request);

    void remove(String token);

    DictionaryResponse getDictionary();
}
