package com.example.restaurant.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.config.RateLimitConfig;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.services.interfaces.IReportServices;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = ReportController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class ReportControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private IReportServices _reportServices;

  @Test
  void add_path() throws Exception {
    mockMvc
        .perform(
            post("/api/report")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"clientToken\":\"CLIENT_1\",\"reason\":\"Nieodpowiednie zachowanie klienta\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/report")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientToken\":\"CLIENT_1\",\"reason\":\"short\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changeStatus_path() throws Exception {
    String statusJson =
        "{\"reportToken\":\"REPORT_1\",\"accepted\":true,\"expiresAt\":\""
            + OffsetDateTime.now().plusDays(2)
            + "\"}";

    mockMvc
        .perform(
            put("/api/report/change-status")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusJson))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing"))
        .when(_reportServices)
        .changeStatus(anyString(), any());
    mockMvc
        .perform(
            put("/api/report/change-status")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusJson))
        .andExpect(status().isNotFound());
  }
}
