package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.jpa.base.IJpaTranslatedRepository;
import java.util.List;

public interface IJpaAllergensRepository extends IJpaTranslatedRepository<Allergens> {
  List<Allergens> findByTokenIn(List<String> tokens);
}
