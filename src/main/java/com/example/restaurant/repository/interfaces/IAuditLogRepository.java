package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.AuditLog;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public interface IAuditLogRepository {
    void save(AuditLog auditLog);
}
