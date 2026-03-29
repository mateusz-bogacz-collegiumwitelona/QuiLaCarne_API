package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Schema(description = "Request object for filtering and paginating the reports list")
public class ReportFilterRequest {
    @Schema(description = "Start date for filtering reports (ISO 8601 format)", example = "2024-01-01T00:00:00Z")
    private OffsetDateTime fromDate;

    @Schema(description = "End date for filtering reports (ISO 8601 format)", example = "2024-12-31T23:59:59Z")
    private OffsetDateTime toDate;

    @Schema(description = "Token of the report status to filter by", example = "status-token-1234")
    private String statusToken;

    @Pattern(regexp = "^(?i)(ASC|DESC)$", message = "Sort direction must be either ASC or DESC")
    @Schema(description = "Sort direction based on creation date", example = "DESC", allowableValues = {"ASC", "DESC"})
    private String sortDirection = "DESC";

    @Valid
    @Schema(description = "Pagination parameters")
    private PaggedRequest pagged = new PaggedRequest();
}
