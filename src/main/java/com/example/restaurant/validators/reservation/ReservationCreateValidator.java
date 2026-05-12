package com.example.restaurant.validators.reservation;

import com.example.restaurant.dto.request.ReservationRequest;

public interface ReservationCreateValidator {
  void validate(ReservationRequest request);
}
