package com.example.restaurant.services;

import com.example.restaurant.dto.domain.LogDomain;
import com.example.restaurant.models.AuditLog;
import com.example.restaurant.models.Users;
import com.example.restaurant.repository.interfaces.IAuditLogRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    void log_ShouldSaveAuditLogWithUser_WhenUserIsKnown() {
        LogDomain logDomain = new LogDomain("testUser", "SOME_ACTION", "127.0.0.1", Map.of());
        Users mockUser = new Users();
        mockUser.setUsername("testUser");

        when(_userRepo.findByNormalizedUsername("TESTUSER")).thenReturn(Optional.of(mockUser));

        _auditLogServices.log(logDomain);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(_auditRepo, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("SOME_ACTION", savedLog.getAction());
        assertEquals("127.0.0.1", savedLog.getIpAddress());
        assertEquals(mockUser, savedLog.getUser());
    }

    @Test
    void log_ShouldSaveAuditLogWithoutUser_WhenUserIsAnonymous() {
        LogDomain logDomain = new LogDomain("anonymousUser", "SOME_ACTION", "127.0.0.1", Map.of());

        _auditLogServices.log(logDomain);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(_auditRepo, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("SOME_ACTION", savedLog.getAction());
        assertNull(savedLog.getUser());

        verify(_userRepo, never()).findByNormalizedUsername(anyString());
    }

    @Test
    void log_ShouldCatchExceptionAndNotThrow_WhenRepositoryFails() {
        LogDomain logDomain = new LogDomain("anonymousUser", "SOME_ACTION", "127.0.0.1", Map.of());

        doThrow(new RuntimeException("Database down")).when(_auditRepo).save(any(AuditLog.class));

        assertDoesNotThrow(() -> _auditLogServices.log(logDomain));
    }
}