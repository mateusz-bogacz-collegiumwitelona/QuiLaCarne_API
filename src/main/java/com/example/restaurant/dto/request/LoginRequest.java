package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username or Email is required")
    @Schema(description = "Username or Email address", example = "admin")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "Admin123!")
    private String password;
}
