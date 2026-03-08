package com.example.restaurant.repository;

import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaDishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DishRepository implements IDishRepository {
    private final IJpaDishRepository _jpaDishRepo;
    private final DishMapper _dishMapper;

    @Override
    public List<DishListResponse> findAllDishes(String lang) {
        return _jpaDishRepo.findAll()
                .stream()
                .map(dish -> _dishMapper.toDishListResponse(dish, lang))
                .toList();
    }
}
