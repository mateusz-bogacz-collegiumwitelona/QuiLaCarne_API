package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.models.lookup.Allergens;

import java.util.List;

public interface IIngredientsRepository {
    void add(AddEntityRequest request, List<Allergens> allergens);
}
