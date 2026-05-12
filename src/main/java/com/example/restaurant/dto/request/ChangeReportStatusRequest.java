package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@Schema(description = "Request object for accepting or rejecting a guest report")
public class ChangeReportStatusRequest {

  @Schema(
      description = "Unique identifier (token) of the report being evaluated",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private String reportToken;

  @Schema(
      description =
          "Indicates whether the report is accepted (true) or rejected (false). If true, a ban is automatically created for the reported guest.",
      example = "true")
  private boolean isAccepted;

  @Schema(
      description =
          "The exact date and time when the ban should expire. Required if the report is accepted.",
      example = "2026-12-31T23:59:59Z")
  private OffsetDateTime expiresAt;
}
