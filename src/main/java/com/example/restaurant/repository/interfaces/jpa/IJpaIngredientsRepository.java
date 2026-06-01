package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.jpa.base.IJpaTranslatedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IJpaIngredientsRepository extends IJpaTranslatedRepository<Ingredients> {
  Page<Ingredients> findByDeletedAtIsNull(Pageable pageable);
}
