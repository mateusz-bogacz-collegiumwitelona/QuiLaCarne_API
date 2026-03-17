package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.domain.LogDomain;

public interface IAuditLogRepository {
    void log(LogDomain logDomain);
}
