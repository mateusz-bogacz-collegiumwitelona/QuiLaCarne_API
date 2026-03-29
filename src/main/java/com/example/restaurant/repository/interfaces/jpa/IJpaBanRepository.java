package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Bans;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IJpaBanRepository extends JpaRepository<Bans, UUID> {
}
