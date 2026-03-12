package com.example.restaurant.dto.request;

import com.example.restaurant.validators.ITimeFramedRequest;
import com.example.restaurant.validators.ValidDates;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@ValidDates
public class ReservationRequest implements ITimeFramedRequest {
    @Valid
    @Parameter(description = "List of dishes token")
    private List<ReservationDishRequest> dishes;

    @NotBlank(message = "Table token is required")
    @Parameter(description = "Token of table ")
    private String tableToken;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(description = "Start time for availability check (ISO 8601, np. 2026-03-10T20:00:00Z)")
    private OffsetDateTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(description = "End time for availability check (ISO 8601, np. 2026-03-10T20:00:00Z)")
    private OffsetDateTime endTime;
}
