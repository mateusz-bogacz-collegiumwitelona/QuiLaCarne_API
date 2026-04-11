package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.lookup.DishesCategories;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IDishRepository {
    Page<Dishes> findAll(Pageable pageable);

    Page<Dishes> findWithoutAllergens(List<String> excludedAllergens, Pageable pageable);

    List<Dishes> listForOrder(List<String> tokens);

    Dishes get(List<Dishes> dishes, String token);

    List<Dishes> findByIngredientsId(UUID id);

    void save(Dishes dish);

    Dishes findByToken(String token);

    DishesCategories findCategoryByToken(String token);

    List<DishesCategories> findAllCategories();

    boolean isCategoryNameTaken(String pl, String en);

    void saveCategory(DishesCategories categorie);

    List<Dishes> findByCategoryId(UUID id);

    long countCategories();

    long count();
}
