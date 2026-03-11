package com.example.restaurant.dto.request;

import lombok.Data;

@Data
public class ReservationDishRequest {
    private String dishToken;
    private int quantity = 1;
    private String note;
}
