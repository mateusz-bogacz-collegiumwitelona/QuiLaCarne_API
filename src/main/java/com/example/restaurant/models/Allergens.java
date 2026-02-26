package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseNamedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "allergens")
@Getter
@Setter
public class Allergens extends BaseNamedEntity {

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "x_dish_composition",
            joinColumns = @JoinColumn(name = "ingredient_id"),
            inverseJoinColumns = @JoinColumn(name = "dish_id")
    )
    private Set<Dishes> dishes = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "x_ingredient_allergens",
            joinColumns = @JoinColumn(name = "ingredient_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id")
    )
    private Set<Allergens> allergens = new HashSet<>();
}
