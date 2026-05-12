package com.example.restaurant.validators.reservation;

import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.repository.interfaces.ITableRespository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TableExistenceValidator implements ReservationCreateValidator {
  private final ITableRespository _tableRepo;

  @Override
  public void validate(ReservationRequest request) {
    if (!_tableRepo.isTableExist(request.getTableToken())) {
      throw new EntityNotFoundException("Table not found");
    }
  }
}
