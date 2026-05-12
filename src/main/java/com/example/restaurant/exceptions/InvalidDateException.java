package com.example.restaurant.exceptions;

public class InvalidDateException extends RuntimeException {
  public InvalidDateException(String message) {
    super(message);
  }
}
