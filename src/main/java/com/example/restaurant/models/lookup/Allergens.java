package com.example.restaurant.models.lookup;

import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "allergens")
@Getter
@Setter
public class Allergens extends BaseTranslatedEntity {

  @ManyToMany(mappedBy = "allergens")
  private Set<Ingredients> ingredients = new HashSet<>();
}
