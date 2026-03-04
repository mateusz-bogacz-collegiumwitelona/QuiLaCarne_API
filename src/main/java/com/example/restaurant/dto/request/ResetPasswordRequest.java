package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @Schema(description = "Token to reset passowrd", example = "adadfafrfwerfwerferfwfs")
    @NotBlank(message = "Token is required ")
    private String token;


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
