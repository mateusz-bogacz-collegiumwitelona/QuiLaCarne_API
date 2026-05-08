package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Bans;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IJpaBanRepository extends JpaRepository<Bans, UUID> {
  List<Bans> findByIsActiveTrueAndExpiresAtBefore(OffsetDateTime time);
}
