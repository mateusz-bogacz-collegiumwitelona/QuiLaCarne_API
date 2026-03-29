package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.GuestReports;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IJpaGuestReportRepository extends JpaRepository<GuestReports, UUID> {
}
