package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.AuditLog;

public interface IAuditLogRepository {
    void save(AuditLog auditLog);
}
