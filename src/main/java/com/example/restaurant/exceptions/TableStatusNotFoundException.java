package com.example.restaurant.exceptions;

public class TableStatusNotFoundException extends RuntimeException {
    public TableStatusNotFoundException(String message) {
        super(message);
    }
}
