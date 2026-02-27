package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseNamedEntity;
import com.example.restaurant.models.lookup.Allergens;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "ingredients")
@Getter
@Setter
public class Ingredients extends BaseNamedEntity {
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "x_dish_composition",
            joinColumns = @JoinColumn(name = "ingredient_id"),
            inverseJoinColumns = @JoinColumn(name = "dish_id")
    )
    private java.util.Set<Dishes> dishes = new java.util.HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "x_ingredient_allergens",
            joinColumns = @JoinColumn(name = "ingredient_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id")
    )
    private java.util.Set<Allergens> allergens = new java.util.HashSet<>();
}
