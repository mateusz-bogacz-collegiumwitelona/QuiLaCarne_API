package com.example.restaurant.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.example.restaurant.models.AuditLog;
import com.example.restaurant.repository.interfaces.jpa.IJpaAuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogRepositoryTest {
  @Mock private IJpaAuditLogRepository _jpaAuditRepo;

  @InjectMocks private AuditLogRepository _auditLogRepo;

  @Test
  @DisplayName("Save log: Success")
  void save_ShouldCallJpaSaveAndFlush() {
    AuditLog log = new AuditLog();
    _auditLogRepo.save(log);

    verify(_jpaAuditRepo, times(1)).saveAndFlush(log);
  }

  @Test
  @DisplayName("Save log: JPA error")
  void save_ShouldThrowException_WhenJpaFails() {
    AuditLog log = new AuditLog();
    doThrow(new RuntimeException("DB Error")).when(_jpaAuditRepo).saveAndFlush(log);

    assertThrows(RuntimeException.class, () -> _auditLogRepo.save(log));
  }
}
