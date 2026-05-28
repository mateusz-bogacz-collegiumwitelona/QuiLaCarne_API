package com.example.restaurant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
  @Schema(description = "JWT Access Token")
  private String token;

  @Schema(description = "Refresh token")
  private String refreshToken;

  @Schema(description = "Authenticated user's login")
  private String username;

  @Schema(description = "List of user Roles")
  private List<String> roles;

  @Schema(description = "Is 2fa enable")
  private boolean requires2fa;
}
