package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "ingredients")
@Getter
@Setter
public class Ingredients extends BaseEntity {
    @Column(name = "name")
    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "dish_composition",
            joinColumns = @JoinColumn(name = "dish_id"),
            inverseJoinColumns = @JoinColumn(name = "  ingredient_id")
    )
    private Set<Dishes> dishes = new HashSet<>();
}
