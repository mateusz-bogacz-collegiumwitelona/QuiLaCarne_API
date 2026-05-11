package com.example.restaurant.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.config.RateLimitConfig;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.services.interfaces.IBanServices;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = BanController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class BanControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private IBanServices _banServices;

  static class CustomPrincipal {
    public String getToken() {
      return "mocked-admin-token";
    }
  }

  @Test
  void create_path() throws Exception {
    String banJson =
        "{\"clientToken\":\"CLIENT_1\",\"reason\":\"Powtarzajace sie naruszenia regulaminu\",\"expiresAt\":\""
            + OffsetDateTime.now().plusDays(1)
            + "\"}";

    TestingAuthenticationToken mockAuth =
        new TestingAuthenticationToken(new CustomPrincipal(), null, "ROLE_MANAGER");

    mockMvc
        .perform(
            post("/api/ban")
                .with(authentication(mockAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(banJson))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing")).when(_banServices).create(anyString(), any());

    mockMvc
        .perform(
            post("/api/ban")
                .with(authentication(mockAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(banJson))
        .andExpect(status().isNotFound());
  }

  @Test
  void getDictionary_path() throws Exception {
    TestingAuthenticationToken mockAuth =
        new TestingAuthenticationToken(new CustomPrincipal(), null, "ROLE_MANAGER");

    mockMvc
        .perform(get("/api/ban/dictionary").with(authentication(mockAuth)))
        .andExpect(status().isOk());

    when(_banServices.getDictionary()).thenThrow(new IllegalStateException("bad"));

    mockMvc
        .perform(get("/api/ban/dictionary").with(authentication(mockAuth)))
        .andExpect(status().isBadRequest());
  }
}
