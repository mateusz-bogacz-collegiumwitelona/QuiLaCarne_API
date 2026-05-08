package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.lookup.Allergens;
import java.util.List;

public interface IAllergensRepository {
  List<Allergens> findAllergens(List<String> allergenTokens);

  List<Allergens> findAll();

  boolean isNameTaken(String pl, String en);

  void save(Allergens allergen);

  Allergens findByToken(String token);

  long count();
}
