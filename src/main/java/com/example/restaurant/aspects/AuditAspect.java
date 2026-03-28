package com.example.restaurant.aspects;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.LogDomain;
import com.example.restaurant.services.interfaces.IAuditLogServices;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final IAuditLogServices _auditLogServices;

    @AfterReturning(value = "@annotation(auditable)")
    public void logAuditActivity(JoinPoint joinPoint, Auditable auditable) {
        Map<String, Object> details = new HashMap<>();
        details.put("method", joinPoint.getSignature().toString());

        LogDomain logDomain = new LogDomain(
                getUsername(),
                auditable.action(),
                getIp(),
                details
        );

        _auditLogServices.log(logDomain);
    }

    private String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) return auth.getName();

        return "anonymousUser";
    }

    private String getIp() {
        if (RequestContextHolder.getRequestAttributes() == null) return "UNKNOWN";

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) return request.getRemoteAddr();

        return xfHeader.split(",")[0];
    }
}
