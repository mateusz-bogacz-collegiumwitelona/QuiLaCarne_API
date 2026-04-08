package com.example.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private List<ReservationDishResponse> dishes;
    private int totalPrice;
    private boolean isActive;
}
