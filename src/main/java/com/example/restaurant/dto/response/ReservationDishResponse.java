package com.example.restaurant.dto.response;

import lombok.Data;

@Data
public class ReservationDishResponse {
    private String dishName;
    private int price;
    private int quantity;
    private String status;
}
