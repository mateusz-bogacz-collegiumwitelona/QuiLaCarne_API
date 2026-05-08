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
public class SyncTableResponse {
  private String token;
  private int tableNumber;
  private int capacity;
  private List<String> statusTokens;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
