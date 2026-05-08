package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.models.lookup.Allergens;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
public class Ingredients extends BaseTranslatedEntity {
  @ManyToMany(mappedBy = "ingredients")
  private Set<Dishes> dishes = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "x_ingredient_allergens",
      joinColumns = @JoinColumn(name = "ingredient_id"),
      inverseJoinColumns = @JoinColumn(name = "allergen_id"))
  private Set<Allergens> allergens = new HashSet<>();
}
