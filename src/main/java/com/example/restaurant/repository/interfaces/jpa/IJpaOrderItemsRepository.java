package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IJpaOrderItemsRepository extends JpaRepository<OrderItems, UUID> {
}
