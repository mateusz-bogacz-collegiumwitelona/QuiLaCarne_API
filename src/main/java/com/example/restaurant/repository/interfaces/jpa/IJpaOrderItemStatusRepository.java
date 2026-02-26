package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.OrderItemsStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IJpaOrderItemStatusRepository extends JpaRepository<OrderItemsStatus, UUID> {
    Optional<OrderItemsStatus> findByName(String name);
}
