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
public class SyncOrderResponse {
  private String token;
  private String reservationToken;
  private String tableToken;
  private String waiterToken;
  private List<String> statusTokens;
  private int totalPrice;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
