package com.example.restaurant.exceptions;

public class GoogleAuthenticationException extends RuntimeException {
    public GoogleAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
