package com.example.restaurant.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class TodayReservationDishResponse {
    private String dishToken;
    private String dishName;
    private int price;
    private int quantity;
    private List<String> ingredient;
    private List<String> allergens;
    private String note;
}
