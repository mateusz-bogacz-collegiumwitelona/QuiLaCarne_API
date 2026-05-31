package com.example.restaurant.exceptions;

public class UserBlockedException extends RuntimeException {
  public UserBlockedException(String message) {
    super(message);
  }
}
