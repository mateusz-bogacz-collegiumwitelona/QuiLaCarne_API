package com.example.restaurant.services;

import com.example.restaurant.dto.domain.LogDomain;
import com.example.restaurant.models.AuditLog;
import com.example.restaurant.models.Users;
import com.example.restaurant.repository.interfaces.IAuditLogRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditLogServicesTest {

    @Mock
    private IAuditLogRepository _auditRepo;
    @Mock
    private IUserRepository _userRepo;
    @InjectMocks
    private AuditLogServices _auditLogServices;

    @Test
    @DisplayName("Audit Log: Success - Save log with user")
    void log_ShouldSaveAuditLogWithUser_WhenUserIsKnown() {
        LogDomain logDomain = new LogDomain("testUser", "ACTION", "127.0.0.1", Map.of());
        Users mockUser = new Users();
        when(_userRepo.findByNormalizedUsername("TESTUSER")).thenReturn(Optional.of(mockUser));

        _auditLogServices.log(logDomain);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(_auditRepo).save(captor.capture());
        assertEquals(mockUser, captor.getValue().getUser());
    }

    @Test
    @DisplayName("Audit Log: Validation - Skip if LogDomain is null")
    void log_ShouldSkip_WhenLogDomainIsNull() {
        _auditLogServices.log(null);
        verify(_auditRepo, never()).save(any());
    }

    @Test
    @DisplayName("Audit Log: Error Handling - Catch exception but don't rethrow")
    void log_ShouldCatchExceptionAndNotThrow_WhenRepositoryFails() {
        LogDomain logDomain = new LogDomain("anonymousUser", "ACTION", "127.0.0.1", Map.of());
        doThrow(new RuntimeException("DB error")).when(_auditRepo).save(any(AuditLog.class));

        assertDoesNotThrow(() -> _auditLogServices.log(logDomain));
    }
}