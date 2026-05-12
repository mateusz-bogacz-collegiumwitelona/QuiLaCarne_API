package com.example.restaurant.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDetailsResponse {
  private String status;
  private OffsetDateTime startTime;
  private OffsetDateTime endTime;
  private List<ReservationDishResponse> dishes;
  private int totalPrice;
}
