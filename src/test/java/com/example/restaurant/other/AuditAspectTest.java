package com.example.restaurant.other;

import com.example.restaurant.TestConstants;
import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.LogDomain;
import com.example.restaurant.helpers.AuditAspect;
import com.example.restaurant.services.interfaces.IAuditLogServices;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {
    @Mock
    private IAuditLogServices _auditLogServices;

    @Mock
    private JoinPoint _joinPoint;

    @Mock
    private Signature _signature;

    @Mock
    private Auditable _auditable;

    @InjectMocks
    private AuditAspect _audit;

    @BeforeEach
    void setUp() {
        when(_joinPoint.getSignature()).thenReturn(_signature);
        when(_signature.toString()).thenReturn(TestConstants.FAKE_METHOD_SIGNATURE);
        when(_auditable.action()).thenReturn(TestConstants.FAKE_ACTION);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Should log activity for Authenticated User with correct IP (no proxy)")
    void logAuditActivity_AuthenticatedUser_NoForwardedHeader() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        TestConstants.FAKE_USERNAME,
                        TestConstants.FAKE_PASSWORD,
                        java.util.List.of()
                )
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        _audit.logAuditActivity(_joinPoint, _auditable);

        ArgumentCaptor<LogDomain> captor = ArgumentCaptor.forClass(LogDomain.class);
        verify(_auditLogServices).log(captor.capture());

        LogDomain logged = captor.getValue();
        assertEquals(TestConstants.FAKE_USERNAME, logged.username());
        assertEquals(TestConstants.FAKE_ACTION, logged.action());
        assertEquals(TestConstants.FAKE_IP, logged.ipAddress());
        assertEquals(TestConstants.FAKE_METHOD_SIGNATURE, logged.details().get("method"));
    }

    @Test
    @DisplayName("Should log activity for Anonymous User and extract IP from X-Forwarded-For")
    void logAuditActivity_AnonymousUser_WithForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 8.8.8.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        _audit.logAuditActivity(_joinPoint, _auditable);

        ArgumentCaptor<LogDomain> captor = ArgumentCaptor.forClass(LogDomain.class);
        verify(_auditLogServices).log(captor.capture());

        LogDomain logged = captor.getValue();
        assertEquals("anonymousUser", logged.username());
        assertEquals("203.0.113.195", logged.ipAddress());
    }
}
