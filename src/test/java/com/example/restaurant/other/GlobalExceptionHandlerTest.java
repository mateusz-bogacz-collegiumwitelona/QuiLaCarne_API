package com.example.restaurant.other;

import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.exceptions.GlobalExceptionHandler;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IAuditLogServices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    @Mock
    private IAuditLogServices _auditLogServices;

    @InjectMocks
    private GlobalExceptionHandler _exceptionHandler;

    @Test
    @DisplayName("handleEntityNotFoundException: Should return 404 NOT FOUND")
    void handleEntityNotFoundException_ShouldReturn404() {
        EntityNotFoundException ex = new EntityNotFoundException("Dish not found in database");

        ResponseEntity<ResultHandler<Object>> response = _exceptionHandler.handleEntityNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Dish not found in database", response.getBody().getMessage());
        assertEquals(404, response.getBody().getStatusCode());
    }

    @Test
    @DisplayName("handleBadRequestExceptions: Should return 400 BAD REQUEST for EntityAlreadyExistsException")
    void handleBadRequestExceptions_ShouldReturn400() {
        EntityAlreadyExistsException ex = new EntityAlreadyExistsException("User email already taken");

        ResponseEntity<ResultHandler<Object>> response = _exceptionHandler.handleBadRequestExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("User email already taken", response.getBody().getMessage());
        assertEquals(400, response.getBody().getStatusCode());
    }

    @Test
    @DisplayName("handleIllegalStateException: Should return 400 BAD REQUEST")
    void handleIllegalStateException_ShouldReturn400() {
        IllegalStateException ex = new IllegalStateException("You cannot ban yourself");

        ResponseEntity<ResultHandler<Object>> response = _exceptionHandler.handleIllegalStateException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("You cannot ban yourself", response.getBody().getMessage());
        assertEquals(400, response.getBody().getStatusCode());
    }
}