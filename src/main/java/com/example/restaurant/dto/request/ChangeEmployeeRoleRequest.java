package com.example.restaurant.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmployeeRoleRequest {
    @NotBlank(message = "Employee token is required")
    private String employeeToken;

    private boolean isAdmin;
}
