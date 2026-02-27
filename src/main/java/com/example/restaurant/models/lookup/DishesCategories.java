package com.example.restaurant.models.lookup;

import com.example.restaurant.models.base.BaseNamedEntity;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dishes_categories")
@Getter
@Setter
public class DishesCategories extends BaseTranslatedEntity {}
