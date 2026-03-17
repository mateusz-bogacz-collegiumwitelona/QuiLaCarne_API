package com.example.restaurant.repository;

import com.example.restaurant.dto.domain.LogDomain;
import com.example.restaurant.models.AuditLog;
import com.example.restaurant.repository.interfaces.IAuditLogRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaAuditLogRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AuditLogRepository implements IAuditLogRepository {
    private final IJpaUserRepository _jpaUserRepo;
    private final IJpaAuditLogRepository _jpaAuditLogRepo;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void log(LogDomain logDomain) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(logDomain.action());
            auditLog.setIpAddress(logDomain.ipAddress());
            auditLog.setDetails(logDomain.details());

            if (logDomain.username() != null && !logDomain.username().equals("anonymousUser"))
                _jpaUserRepo.findByNormalizedUsername(logDomain.username().toUpperCase())
                        .ifPresent(auditLog::setUser);

            _jpaAuditLogRepo.saveAndFlush(auditLog);
            log.info("Audit log saved async for action: {}", logDomain.action());
        } catch (Exception ex) {
            log.error("Failed to save audit log: {}", ex.getMessage(), ex);
        }
    }
}
