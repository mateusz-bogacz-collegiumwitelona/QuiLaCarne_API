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
public class SyncReservationResponse {
  private String token;
  private String userToken;
  private String tableToken;
  private List<String> statusTokens;
  private OffsetDateTime startTime;
  private OffsetDateTime endTime;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
