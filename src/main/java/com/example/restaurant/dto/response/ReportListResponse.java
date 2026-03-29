package com.example.restaurant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed representation of a single guest report in the list")
public class ReportListResponse {

    @Schema(description = "Unique identifier (token) of the report", example = "550e8400-e29b-41d4-a716-446655440000")
    private String token;

    @Schema(description = "Username of the guest who was reported", example = "john_doe")
    private String guestUsername;

    @Schema(description = "Unique identifier (token) of the reported guest", example = "330e8400-e29b-41d4-a716-116655440000")
    private String guestToken;

    @Schema(description = "Username of the staff member (e.g., waiter) who created the report", example = "waiter_anna")
    private String reporterUsername;

    @Schema(description = "Unique identifier (token) of the reporter", example = "110e8400-e29b-41d4-a716-226655440000")
    private String reporterToken;

    @Schema(description = "The detailed reason for the report", example = "Guest was extremely rude and broke a glass.")
    private String reason;

    @Schema(description = "The exact date and time when the report was created", example = "2024-03-29T18:30:00Z")
    private OffsetDateTime createdAt;

    @Schema(description = "The translated current status of the report", example = "In progress")
    private String status;
}