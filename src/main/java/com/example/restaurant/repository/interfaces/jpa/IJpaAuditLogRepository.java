package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.AuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IJpaAuditLogRepository extends JpaRepository<AuditLog, UUID> {}
