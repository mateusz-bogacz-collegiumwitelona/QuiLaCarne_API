package com.example.restaurant.interceptors;

import com.example.restaurant.services.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {
  private final IdempotencyService _idempotencyService;
  private static final String IDEMPOTENCY_HEADER = "X-Request-ID";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (HttpMethod.GET.matches(request.getMethod())
        || HttpMethod.OPTIONS.matches(request.getMethod())) {
      return true;
    }

    String requestId = request.getHeader(IDEMPOTENCY_HEADER);

    if (requestId == null || requestId.trim().isEmpty()) {
      return true;
    }

    if (_idempotencyService.isProcessed(requestId)) {
      response.setStatus(HttpServletResponse.SC_OK);
      return false;
    }

    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    if (HttpMethod.GET.matches(request.getMethod())
        || HttpMethod.OPTIONS.matches(request.getMethod())) {
      return;
    }

    String requestId = request.getHeader(IDEMPOTENCY_HEADER);

    if (requestId != null && !requestId.trim().isEmpty()) {
      int status = response.getStatus();
      if (status >= 200 && status < 300 && ex == null) {
        _idempotencyService.markAsProcessed(requestId);
      }
    }
  }
}
