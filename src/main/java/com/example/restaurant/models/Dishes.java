package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.models.lookup.DishesCategories;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dishes")
@Getter @Setter
public class Dishes extends BaseTranslatedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private DishesCategories category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int price;
    private boolean isAvailable;

    @Column(name = "unavailable_reason", columnDefinition = "TEXT")
    private String unavailableReason;

    @ManyToMany(mappedBy = "dishes")
    private java.util.Set<Ingredients> ingredients = new java.util.HashSet<>();
}
