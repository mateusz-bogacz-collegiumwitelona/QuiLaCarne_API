package com.example.restaurant.dto.response;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientReservationResponse {
  private String token;
  private OffsetDateTime startTime;
  private OffsetDateTime endTime;
  private String status;
}
