package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@Schema(description = "Request object for manually banning a client")
public class CreateBanRequest {

  @NotBlank(message = "Client token is required")
  @Schema(
      description = "Unique identifier (token) of the client to be banned",
      example = "330e8400-e29b-41d4-a716-116655440000",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String clientToken;

  @NotBlank(message = "Reason for the ban is required")
  @Size(max = 500, message = "Reason cannot exceed 500 characters")
  @Schema(
      description =
          "The detailed reason for the ban. This will be included in the email sent to the user.",
      example = "Repeated aggressive behavior towards staff.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String reason;

  @NotNull(message = "Expiration date is required")
  @Future(message = "The ban expiration date must be in the future")
  @Schema(
      description = "The exact date and time when the ban should expire",
      example = "2026-12-31T23:59:59Z",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private OffsetDateTime expiresAt;
}
