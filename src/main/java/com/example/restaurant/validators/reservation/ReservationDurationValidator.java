package com.example.restaurant.validators.reservation;

import com.example.restaurant.dto.request.ReservationRequest;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class ReservationDurationValidator implements ReservationCreateValidator {
  @Override
  public void validate(ReservationRequest request) {
    Duration duration = Duration.between(request.getStartTime(), request.getEndTime());

    if (duration.toMinutes() < 30) {
      throw new IllegalStateException("Reservation must be at least 30 minutes long");
    }

    if (duration.toHours() > 3) {
      throw new IllegalStateException("Reservation cannot exceed 3 hours");
    }
  }
}
