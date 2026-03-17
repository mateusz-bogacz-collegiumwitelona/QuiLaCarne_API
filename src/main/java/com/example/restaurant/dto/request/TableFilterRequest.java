package com.example.restaurant.dto.request;

import com.example.restaurant.annotations.ValidDates;
import com.example.restaurant.validators.ITimeFramedRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Data
@ValidDates
public class TableFilterRequest implements ITimeFramedRequest {
    @Parameter(description = "Start time for availability check (ISO 8601, np. 2026-03-10T18:00:00Z)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startTime;

    @Parameter(description = "End time for availability check (ISO 8601, np. 2026-03-10T20:00:00Z)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endTime;
}
