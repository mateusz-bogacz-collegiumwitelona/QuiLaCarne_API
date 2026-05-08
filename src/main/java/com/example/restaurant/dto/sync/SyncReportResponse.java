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
public class SyncReportResponse {
  private String token;
  private String guestToken;
  private String reporterToken;
  private List<String> statusTokens;
  private String reason;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
