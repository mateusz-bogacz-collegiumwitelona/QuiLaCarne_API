package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.RestaurantTables;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IJpaTableRepository extends JpaRepository<RestaurantTables, UUID> {
}
