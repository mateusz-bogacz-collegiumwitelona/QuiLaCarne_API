package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.GuestReports;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IJpaGuestReportRepository
    extends JpaRepository<GuestReports, UUID>, JpaSpecificationExecutor<GuestReports> {
  GuestReports findByToken(String token);
}
