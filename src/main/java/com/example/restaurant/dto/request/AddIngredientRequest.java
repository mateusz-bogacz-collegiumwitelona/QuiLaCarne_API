package com.example.restaurant.dto.request;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class AddIngredientRequest {
    @Valid
    private AddEntityRequest entity;
    private Set<String> allergenTokens = new HashSet<>();
}
