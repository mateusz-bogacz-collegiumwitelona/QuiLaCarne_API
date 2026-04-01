package com.example.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlockEmployeeRequest {
    @NotBlank(message = "Employee token is required")
    private String employeeToken;

    private boolean isAvailable;
}
