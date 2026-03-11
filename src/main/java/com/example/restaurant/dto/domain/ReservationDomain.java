package com.example.restaurant.dto.domain;

import java.util.List;

public record ReservationDomain(
        List<ReservationDishDoamin> dishes,
        int totalPrice
) {
}
