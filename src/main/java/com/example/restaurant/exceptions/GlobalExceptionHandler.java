package com.example.restaurant.exceptions;

import com.example.restaurant.helpers.ResultHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidDateException.class)
    public ResponseEntity<ResultHandler<Object>> handleInvalidDateException(InvalidDateException ide) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResultHandler.failure(
                                ide.getMessage(),
                                HttpStatus.BAD_REQUEST.value()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultHandler<Object>> handleValidationExceptions(MethodArgumentNotValidException manve) {
        List<String> errors = manve.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getDefaultMessage())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResultHandler.failure("Validation failed", HttpStatus.BAD_REQUEST.value(), errors)
        );
    }
}