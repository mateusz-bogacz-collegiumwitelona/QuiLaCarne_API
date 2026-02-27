package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.jpa.base.IJpaTranslatedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IJpaIngredientsRepository extends IJpaTranslatedRepository<Ingredients> {}
