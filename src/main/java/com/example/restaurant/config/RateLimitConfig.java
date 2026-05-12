package com.example.restaurant.config;

import com.example.restaurant.enums.RateLimitEnum;
import com.example.restaurant.helpers.ResultHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RateLimitConfig extends OncePerRequestFilter {
  private final ProxyManager<byte[]> _proxyManager;
  private final ObjectMapper _objectMapper;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    byte[] key = getClientKey(request).getBytes(StandardCharsets.UTF_8);

    RateLimitEnum plan = resolve();

    Bucket bucket = _proxyManager.builder().build(key, plan::getConfiguration);

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);

      ResultHandler<Void> result =
          ResultHandler.failure(
              "Too many requests. Please try again later.", HttpStatus.TOO_MANY_REQUESTS.value());

      response.getWriter().write(_objectMapper.writeValueAsString(result));
    }
  }

  private String getClientKey(HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
      return auth.getName();

    return request.getRemoteAddr();
  }

  private RateLimitEnum resolve() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated()) return RateLimitEnum.GUEST;

    return auth.getAuthorities().stream()
            .anyMatch(
                a ->
                    "ROLE_MANAGER".equals(a.getAuthority())
                        || "ROLE_WAITER".equals(a.getAuthority()))
        ? RateLimitEnum.STAFF
        : RateLimitEnum.CLIENT;
  }
}
