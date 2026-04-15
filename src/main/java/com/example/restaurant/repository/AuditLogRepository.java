package com.example.restaurant.repository;

import com.example.restaurant.models.AuditLog;
import com.example.restaurant.repository.interfaces.IAuditLogRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class AuditLogRepository implements IAuditLogRepository {
    private final IJpaAuditLogRepository _jpaAuditLogRepo;

    @Override
    public void save(AuditLog auditLog) {
        _jpaAuditLogRepo.saveAndFlush(auditLog);
    }
}
