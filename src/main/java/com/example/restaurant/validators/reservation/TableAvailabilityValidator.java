package com.example.restaurant.validators.reservation;

import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.repository.interfaces.ITableRespository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TableAvailabilityValidator implements ReservationCreateValidator {
  private final ITableRespository _tableRepo;

  @Override
  public void validate(ReservationRequest request) {
    if (!_tableRepo.isTableAvailable(
        request.getTableToken(), request.getStartTime(), request.getEndTime())) {
      throw new IllegalStateException("Table is already reserved for this time slot");
    }
  }
}
