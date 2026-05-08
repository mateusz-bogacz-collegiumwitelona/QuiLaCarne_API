package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IJpaOrderRepository extends JpaRepository<Orders, UUID> {
  Optional<Orders> findByReservation_Token(String reservationToken);

  List<Orders> findByStatusesContaining(OrderStatus status);
}
