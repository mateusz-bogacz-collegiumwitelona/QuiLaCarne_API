package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.EntityResponse;

import java.util.List;

public interface IAllergensServices {
    List<EntityResponse> getDictionary();

    void add(AddEntityRequest request);

    void remove(String token);
}
