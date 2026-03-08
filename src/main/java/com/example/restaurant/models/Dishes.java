package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseNamedEntity;
import com.example.restaurant.models.lookup.DishesCategories;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "dishes")
@Getter @Setter
public class Dishes extends BaseNamedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private DishesCategories category;

    private int price;
    private boolean isAvailable;

    @Column(name = "unavailable_reason", columnDefinition = "TEXT")
    private String unavailableReason;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "x_dish_composition",
            joinColumns = @JoinColumn(name = "dish_id"),
            inverseJoinColumns = @JoinColumn(name = "ingredient_id")
    )
    private Set<Ingredients> ingredients = new HashSet<>();
}
