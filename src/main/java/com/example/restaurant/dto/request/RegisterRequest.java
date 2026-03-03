package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Schema(description = "Unique username for the account", example = "Mati_99")
    @NotBlank(message = "Username is required")
    private String username;

    @Schema(description = "Valid email address for account activation", example = "mati@example.pl")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Password (min. 6 chars, 1 uppercase, 1 digit, 1 special)", example = "SecurePass123!")
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters long")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\"{}|<>]).+$",
            message = "Password must contain at least one uppercase letter, one number, and one special character"
    )
    private String password;

    @Schema(description = "Must match the password field", example = "SecurePass123!")
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
