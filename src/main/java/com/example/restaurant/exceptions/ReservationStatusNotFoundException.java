package com.example.restaurant.exceptions;

public class ReservationStatusNotFoundException extends RuntimeException {
    public ReservationStatusNotFoundException(String message) {
        super(message);
    }
}
