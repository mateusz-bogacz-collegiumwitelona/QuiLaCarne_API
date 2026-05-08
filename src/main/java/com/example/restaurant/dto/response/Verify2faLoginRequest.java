package com.example.restaurant.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Verify2faLoginRequest {
  @NotBlank(message = "Pre-Auth token is required")
  private String preAuthToken;

  @NotNull(message = "2FA code is required")
  private Integer code;
}
