package com.example.restaurant.dto.request;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class AddEmployeeRequest {
    @Valid
    private RegisterRequest register;

    private boolean isAdmin;
}
