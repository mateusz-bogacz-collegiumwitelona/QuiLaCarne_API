package com.example.restaurant.exceptions;

import com.example.restaurant.dto.domain.LogDomain;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IAuditLogServices;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final IAuditLogServices _auditLogServices;

    @ExceptionHandler({
            InvalidDateException.class,
            EntityAlreadyExistsException.class,
    })
    public ResponseEntity<ResultHandler<Object>> handleInvalidDateException(RuntimeException rex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResultHandler.failure(
                                rex.getMessage(),
                                HttpStatus.BAD_REQUEST.value()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultHandler<Object>> handleValidationExceptions(MethodArgumentNotValidException manvex) {
        List<String> errors = manvex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> String.format("%s: %s", err.getField(), err.getDefaultMessage()))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResultHandler.failure("Validation failed", HttpStatus.BAD_REQUEST.value(), errors)
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultHandler<Object>> handleGenericException(Exception ex) {
        log.error("CRITICAL UNHANDLED EXCEPTION: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ResultHandler.failure(
                        "An unexpected error occurred. Please contact support.",
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                )
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResultHandler<Object>> handleAuthenticationException(
            AuthenticationException aex,
            HttpServletRequest request) {

        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        } else {
            ipAddress = ipAddress.split(",")[0];
        }

        Map<String, Object> details = new HashMap<>();
        details.put("error_message", aex.getMessage());
        details.put("path", request.getRequestURI());

        LogDomain logDomain = new LogDomain(
                "UNKNOWN",
                "FAILED_LOGIN",
                ipAddress,
                details
        );

        _auditLogServices.log(logDomain);

        log.warn("Failed login attempt from IP: {}. Reason: {}", ipAddress, aex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ResultHandler.failure(
                        "Invalid credentials",
                        HttpStatus.UNAUTHORIZED.value(),
                        List.of(aex.getMessage())
                )
        );
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResultHandler<Object>> handleIllegalStateException(IllegalStateException isex) {
        log.warn("Business rule violation: {}", isex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResultHandler.failure(
                        isex.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                )
        );
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            ReservationNotFoundException.class,
            StatusNotFoundException.class,
    })
    public ResponseEntity<ResultHandler<Object>> handleResourceNotFoundException(RuntimeException rex) {
        log.warn("Resource not found: {}", rex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResultHandler.failure("Resource not found", HttpStatus.NOT_FOUND.value())
        );
    }

    
}