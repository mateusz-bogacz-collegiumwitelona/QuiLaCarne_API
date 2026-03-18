package com.example.restaurant.dto.domain;

import com.example.restaurant.dto.response.TodayReservationDishResponse;

import java.util.List;

public record TodayOrderSummaryDomain(
        int totalPrice,
        List<TodayReservationDishResponse> dishes
) {
}
