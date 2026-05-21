package com.example.restaurant.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.services.IdempotencyService;
import com.example.restaurant.services.JwtServices;
import com.example.restaurant.services.interfaces.IAuditLogServices;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

abstract class AbstractControllerWebMvcTest {

  @Autowired protected MockMvc mockMvc;

  @MockitoBean protected IAuditLogServices auditLogServices;

  @MockitoBean protected JwtServices jwtServices;

  @MockitoBean protected ProxyManager<byte[]> proxyManager;

  @MockitoBean protected IdempotencyService idempotencyService;

  protected RequestPostProcessor auth() {
    return authWithRoles("ROLE_MANAGER", "ROLE_WAITER", "ROLE_CLIENT");
  }

  protected RequestPostProcessor authWithRoles(String... roles) {
    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(
            new TokenPrincipal("user-token"), null, AuthorityUtils.createAuthorityList(roles));
    return authentication(token);
  }

  protected RequestPostProcessor managerAuth() {
    return authWithRoles("ROLE_MANAGER");
  }

  protected RequestPostProcessor waiterAuth() {
    return authWithRoles("ROLE_WAITER");
  }

  protected RequestPostProcessor clientAuth() {
    return authWithRoles("ROLE_CLIENT");
  }

  protected void expectUnauthorizedOrForbidden(ResultActions resultActions) throws Exception {
    resultActions.andExpect(
        result -> {
          int status = result.getResponse().getStatus();
          assertTrue(status == 401 || status == 403, "Expected 401 or 403, got: " + status);
        });
  }

  protected AuthResponse authResponse() {
    return AuthResponse.builder()
        .token("access-token")
        .refreshToken("refresh-token")
        .username("tester")
        .requires2fa(false)
        .build();
  }

  protected record TokenPrincipal(String token) {}
}
