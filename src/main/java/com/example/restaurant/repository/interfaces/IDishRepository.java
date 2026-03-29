package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.models.Dishes;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface IDishRepository {
    Page<Dishes> findAllDishes(DishFilterRequest request, PaggedRequest pagged);

    List<Dishes> listForOrder(List<String> tokens);

    Dishes get(List<Dishes> dishes, String token);

    List<Dishes> findByIngredientsId(UUID id);

    void save(Dishes dish);
}
