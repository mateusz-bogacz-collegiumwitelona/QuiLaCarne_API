package com.example.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDishResponse {
    private String dishName;
    private int price;
    private int quantity;
    private String status;
}
