package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IJpaOrederStatusRepositry extends JpaRepository<OrderStatus, UUID> {
    Optional<OrderStatus> findByName(String name);
}
