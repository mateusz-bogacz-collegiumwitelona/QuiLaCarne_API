package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.AddEntityRequest;

public interface IIngredientsRepository {
    void add(AddEntityRequest request);
}
