package com.example.restaurant.validators;

import java.time.OffsetDateTime;

public interface ITimeFramedRequest {
  OffsetDateTime getStartTime();

  OffsetDateTime getEndTime();
}
