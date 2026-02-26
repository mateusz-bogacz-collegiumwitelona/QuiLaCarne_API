package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseNamedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "allergens")
@Getter @Setter
public class Allergens extends BaseNamedEntity {

    @ManyToMany(mappedBy = "allergens")
    private Set<Ingredients> ingredients = new HashSet<>();
}
