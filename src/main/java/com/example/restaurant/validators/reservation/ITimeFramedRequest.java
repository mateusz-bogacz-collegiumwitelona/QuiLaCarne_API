package com.example.restaurant.validators.reservation;

import java.time.OffsetDateTime;

public interface ITimeFramedRequest {
  OffsetDateTime getStartTime();

  OffsetDateTime getEndTime();
}
