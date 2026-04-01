package com.example.restaurant.dto.request;

import lombok.Data;

@Data
public class AddTableRequest {
    private int tableNumber;
    private int capacity;
}
