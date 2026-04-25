package com.example.restaurant.services;

import com.example.restaurant.dto.domain.LogDomain;
import com.example.restaurant.models.AuditLog;
import com.example.restaurant.repository.interfaces.IAuditLogRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IAuditLogServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServices implements IAuditLogServices {
    private final IAuditLogRepository _auditRepo;
    private final IUserRepository _userRepo;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void log(LogDomain logDomain) {
        if (logDomain == null || logDomain.action() == null) {
            log.warn("Attempted to save an empty audit log. Skipping.");
            return;
        }

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(logDomain.action());
            auditLog.setIpAddress(logDomain.ipAddress());
            auditLog.setDetails(logDomain.details());

            if (logDomain.username() != null && !"anonymousUser".equals(logDomain.username()))
                _userRepo.findByNormalizedUsername(logDomain.username().toUpperCase())
                        .ifPresent(auditLog::setUser);

            _auditRepo.save(auditLog);
            if (log.isInfoEnabled()) {
                log.info("Audit log saved async for action: {}", logDomain.action());
            }
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Failed to save audit log for action {}: {}", logDomain.action(), ex.getMessage());
            }
        }
    }

}
