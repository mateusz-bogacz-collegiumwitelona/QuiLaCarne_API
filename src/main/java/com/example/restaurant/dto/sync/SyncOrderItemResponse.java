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
public class SyncOrderItemResponse {
  private String token;
  private String orderToken;
  private String productToken;
  private List<String> statusTokens;
  private int quantity;
  private int priceAtTimeOfOrder;
  private String note;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
