package com.example.restaurant.repository;

import com.example.restaurant.models.AuditLog;
import com.example.restaurant.repository.interfaces.jpa.IJpaAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuditLogRepositoryTest {
    @Mock
    private IJpaAuditLogRepository _jpaAuditRepo;

    @InjectMocks
    private AuditLogRepository _auditLogRepo;

    @Test
    void save_ShouldCallJpaSaveAndFlush() {
        AuditLog log = new AuditLog();
        _auditLogRepo.save(log);

        verify(_jpaAuditRepo, times(1)).saveAndFlush(log);
    }
}
