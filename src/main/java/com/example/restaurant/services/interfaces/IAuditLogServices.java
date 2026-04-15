package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.LogDomain;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public interface IAuditLogServices {
    void log(LogDomain logDomain);
}
