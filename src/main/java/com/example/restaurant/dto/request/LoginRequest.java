package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginRequest {
    @Schema(description = "Username or Email address", example = "admin")
    private String username;

    @Schema(description = "User password", example = "Admin123!")
    private String password;
}
