package com.example.restaurant.validators;

import com.example.restaurant.dto.request.ReservationRequest;

public interface ReservationCreateValidator {
  void validate(ReservationRequest request);
}
