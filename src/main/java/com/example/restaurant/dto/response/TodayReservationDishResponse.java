package com.example.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodayReservationDishResponse {
    private String dishToken;
    private String dishName;
    private int price;
    private int quantity;
    private List<String> ingredient;
    private List<String> allergens;
    private String note;
}
