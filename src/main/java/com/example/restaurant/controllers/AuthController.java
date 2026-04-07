package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.dto.response.Verify2faLoginRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/api/auth", produces = "application/json")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthServices _authServices;

    @Operation(
            summary = "Authenticate user (Login Step 1)",
            description = "Authenticates a user using their username (or email) and password. " +
                    "If credentials are valid and 2FA is OFF, a final JWT token is returned. " +
                    "If 2FA is ON, a Pre-Auth token is returned and the 'requires2fa' flag is set to true. " +
                    "In that case, the client must proceed to the /verify-2fa endpoint.",
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
                    description = "Login successful. " +
                            "Returns either a final JWT Token or a Pre-Auth token if 2FA is required.",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "401", description = "Invalid username/email or password"),
            @ApiResponse(responseCode = "403", description = "Account is disabled or has no permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/login")
    public ResponseEntity<ResultHandler<AuthResponse>> login(@RequestBody LoginRequest request) {
        var response = _authServices.authenticate(request);
        return ResponseEntity.ok(ResultHandler.success(
                "Login processed successfully",
                HttpStatus.OK.value(),
                response
        ));
    }

    @Operation(
            summary = "Logout user",
            description = "Logs out the authenticated user by revoking their refresh token. " +
                    "The client application must also delete the JWT and Refresh Token locally."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logged out successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/logout")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultHandler<Void>> logout(
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        _authServices.logout(userToken);

        return ResponseEntity.ok(ResultHandler.success(
                "Logged out successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Register a new customer account (Step 1)",
            description = "Creates a new account with 'isActive' set to false and " +
                    "sends an activation email containing a unique token. " +
                    "The user must call the /confirm endpoint with this token to enable the account.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User registered successfully, activation email sent"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input data, password mismatch or username taken"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Server error or email delivery failure"
                    )
            }
    )
    @PostMapping("/register")
    public ResponseEntity<ResultHandler<Void>> register(@RequestBody @Valid RegisterRequest request) {
        _authServices.register(request);
        return ResponseEntity.ok(ResultHandler.success(
                "User registered successfully",
                HttpStatus.CREATED.value()
        ));
    }

    @Operation(
            summary = "Activate account with email token (Step 2)",
            description = "Finalizes the registration process by activating the user account. " +
                    "Expects a unique token received via email. Once activated, the user can log in.",
            parameters = {
                    @Parameter(
                            name = "token",
                            description = "The unique activation token from the email",
                            required = true,
                            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account activated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid, expired or already used token"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/register-confirm")
    public ResponseEntity<ResultHandler<Boolean>> registerConfirm(@RequestParam String token) {
        boolean response = _authServices.registerConfirm(token);
        return ResponseEntity.ok(ResultHandler.success(
                "User activated successfully",
                HttpStatus.OK.value(),
                response
        ));
    }

    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset link to the provided email address if the account exists. " +
                    "For privacy reasons, always returns a success message."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request processed")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ResultHandler<Void>> resetPassword(@RequestParam String email) {
        _authServices.resetPassword(email);
        return ResponseEntity.ok(ResultHandler.success(
                "If account exists, a link was sent.",
                HttpStatus.OK.value()
        ));
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
    public ResponseEntity<ResultHandler<Boolean>> setNewPassword(@RequestBody ResetPasswordRequest request) {
        var result = _authServices.setNewPassword(request);
        return ResponseEntity.ok(ResultHandler.success(
                "Reset password successfully",
                HttpStatus.OK.value(),
                result
        ));
    }

    @Operation(
            summary = "Continue with Google (Login/Register)",
            description = "Accepts a Google ID Token. If the user doesn't exist, " +
                    "an account is created automatically. " +
                    "Returns a standard JWT Bearer token."
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
    @PostMapping("/google")
    public ResponseEntity<ResultHandler<AuthResponse>> googleAuth(@Valid @RequestBody GoogleLoginRequest request) {
        var response = _authServices.authenticateWithGoogle(request);
        return ResponseEntity.ok(ResultHandler.success(
                "Login successful",
                HttpStatus.OK.value(),
                response
        ));
    }

    @Operation(
            summary = "Verify 2FA code and get JWT Token",
            description = "Validates the 6-digit 2FA code along with the Pre-Auth token. " +
                    "If valid, returns the final JWT Bearer token giving full access to the system."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "2FA verified successfully, JWT token returned",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid 2FA code"),
            @ApiResponse(responseCode = "401", description = "Pre-Auth token is invalid or expired"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/verify-2fa")
    public ResponseEntity<ResultHandler<AuthResponse>> verify2faLogin(
            @Valid @RequestBody Verify2faLoginRequest request
    ) {
        var response = _authServices.verify2faLogin(request);
        return ResponseEntity.ok(ResultHandler.success(
                "2FA verification successful",
                HttpStatus.OK.value(),
                response
        ));
    }

    @Operation(
            summary = "Refresh Access Token",
            description = "Exchanges a valid Refresh Token for a new pair of Access and Refresh tokens."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request format"),
            @ApiResponse(responseCode = "401", description = "Refresh token is invalid or expired"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ResultHandler<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        var response = _authServices.refreshToken(request);
        return ResponseEntity.ok(ResultHandler.success(
                "Token refreshed successfully",
                HttpStatus.OK.value(),
                response
        ));
    }
}
