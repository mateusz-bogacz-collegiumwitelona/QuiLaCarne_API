package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.Ingredients;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IIngredientsRepository {
    void save(Ingredients ingredients);

    boolean isNameTaken(String pl, String en);

    Ingredients findByToken(String token);

    List<Ingredients> findAll();

    Page<Ingredients> findAll(Pageable pageable);

    long count();
}
