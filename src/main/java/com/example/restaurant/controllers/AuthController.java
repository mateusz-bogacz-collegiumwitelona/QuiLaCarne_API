package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.ResetPasswordRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IAuthServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(
            summary = "Register a new customer account (Step 1)",
            description = "Creates a new account with 'isActive' set to false and sends an activation email containing a unique token. " +
                    "The user must call the /confirm endpoint with this token to enable the account.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User registered successfully, activation email sent"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data, password mismatch or username taken"),
                    @ApiResponse(responseCode = "500", description = "Server error or email delivery failure")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<ResultHandler<String>> register(
            @RequestBody
            @Valid
            RegisterRequest request)
    {
        var result = _authServices.register(request);

        return ResponseEntity.status(result.getStatusCode()).body(result);
    }

    @Operation(
            summary = "Activate account with email token (Step 2)",
            description = "Finalizes the registration process by activating the user account. " +
                    "Expects a unique token received via email. Once activated, the user can log in.",
            parameters = {
                    @Parameter(name = "token", description = "The unique activation token from the email", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account activated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid, expired or already used token"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/register-confirm")
    public ResponseEntity<ResultHandler<String>> registerConfirm(@RequestParam String token)
    {
        var result = _authServices.registerConfirm(token);
        return ResponseEntity.status(result.getStatusCode()).body(result);
    }

    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset link to the provided email address if the account exists. For privacy reasons, always returns a success message."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request processed")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ResultHandler<String>> resetPassword(@RequestParam String email)
    {
        var result = _authServices.resetPassowrd(email);
        return ResponseEntity.status(result.getStatusCode()).body(result);
    }

    @Operation(
            summary = "Set new password",
            description = "Updates the user's password using the reset token received via email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token or password mismatch")
    })
    @PostMapping("/set-password")
    public ResponseEntity<ResultHandler<String>> setNewPassword(@RequestBody ResetPasswordRequest request)
    {
        var result = _authServices.setNewPassword(request);
        return ResponseEntity.status(result.getStatusCode()).body(result);
    }
}
