package com.example.restaurant.dto.response;

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
public class TodayReservationsResponse {
  private String token;
  private String username;
  private OffsetDateTime startTime;
  private OffsetDateTime endTime;
  private String status;
  private int tableNumber;
  private String tableStatus;
  private List<TodayReservationDishResponse> dishes;
  private int totalPrice;
}
