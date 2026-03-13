package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IJpaOrderRepository extends JpaRepository<Orders, UUID> {
    Optional<Orders> findByReservation_Token(String reservationToken);
}
