package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IJpaAuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
