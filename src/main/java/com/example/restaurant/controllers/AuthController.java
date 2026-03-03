package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IAuthServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthServices _authServices;

    @Operation(
            summary = "Authenticate user and receive a JWT Bearer token",
            description = "Authenticates a user using their username (or email) and password. " +
                    "If credentials are valid, a JWT token is returned in the response body. " +
                    "This token must be included in the 'Authorization: Bearer <token>' header for all protected requests.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Admin Login",
                                            summary = "Login for Manager role",
                                            value = "{\"username\": \"admin\", \"password\": \"Admin123!\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Waiter Login",
                                            summary = "Login for Waiter role",
                                            value = "{\"username\": \"waiter\", \"password\": \"Waiter123!\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Client Login",
                                            summary = "Login for Client role",
                                            value = "{\"username\": \"client\", \"password\": \"Client123!\"}"
                                    )
                            }
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully logged in, token returned",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "401", description = "Invalid username/email or password"),
            @ApiResponse(responseCode = "403", description = "Account is disabled or has no permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/login")
    public ResponseEntity<ResultHandler<AuthResponse>> login(@RequestBody LoginRequest request)
    {
        var result = _authServices.authenticate(request);

        return ResponseEntity.status(result.getStatusCode()).body(result);
    }

    @PostMapping("/register")
    public ResponseEntity<ResultHandler<String>> register(
            @RequestBody
            @Valid
            RegisterRequest request)
    {
        var result = _authServices.register(request);

        return ResponseEntity.status(result.getStatusCode()).body(result);
    }
}
