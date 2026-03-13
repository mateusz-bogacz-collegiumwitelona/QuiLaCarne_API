package com.example.restaurant.dto.domain;

import com.example.restaurant.dto.response.ReservationDishResponse;

import java.util.List;

public record OrderSummaryDomain(
        int totalPrice,
        List<ReservationDishResponse> dishes
) {
}
