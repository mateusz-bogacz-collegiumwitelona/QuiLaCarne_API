package com.example.restaurant.exceptions;

public class FileProcessingException extends RuntimeException {
  public FileProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
