package com.example.restaurant.dto.response;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableListResponse {
  private String token;
  private int tableNumber;
  private int capacity;
  private String status;
  private OffsetDateTime updatedAt;
}
