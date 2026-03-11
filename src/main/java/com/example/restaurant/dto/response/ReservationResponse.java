package com.example.restaurant.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ReservationResponse {
    private List<ReservationDishResponse> dishes;
    private int totalPrice;
    private boolean isActive;
}
