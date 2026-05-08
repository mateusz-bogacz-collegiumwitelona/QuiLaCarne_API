package com.example.restaurant.dto.sync;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncUserResponse {
  private String token;
  private String username;
  private String email;
  private Boolean isActive;
  private boolean isStaff;
  private List<String> roleTokens;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
