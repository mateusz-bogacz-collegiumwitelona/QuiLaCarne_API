package com.example.restaurant.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
  private String username;
  private String email;
  private boolean is2FaEnable;
  private List<String> roles;
}
