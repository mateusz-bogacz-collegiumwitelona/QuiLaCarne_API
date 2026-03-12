package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationDishRequest {
    @NotBlank
    @Parameter(description = "Dish token")
    private String dishToken;

    @NotNull
    @Parameter(description = "Quantity of dish")
    @Min(value = 1, message = "Must be get min 1 quantity")
    private int quantity = 1;

    private String note;
}
