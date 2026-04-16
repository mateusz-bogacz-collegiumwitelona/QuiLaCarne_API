package com.example.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishResponse {
    private String name;
    private int price;
    private String imageUrl;
    private List<String> ingridents;
    private List<String> allergens;
}
