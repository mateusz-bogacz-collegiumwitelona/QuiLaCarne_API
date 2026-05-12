package com.example.restaurant.controllers;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.config.RateLimitConfig;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.services.interfaces.ISystemServices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = SystemController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class SystemControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private ISystemServices _systemServices;

  @Test
  void clearAllCaches_path() throws Exception {
    mockMvc
        .perform(delete("/api/system/cache/clear-all").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new IllegalStateException("boom")).when(_systemServices).clearAllCache();
    mockMvc
        .perform(delete("/api/system/cache/clear-all").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void clearSpecificCache_path() throws Exception {
    mockMvc
        .perform(delete("/api/system/cache/clear/usersList").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("Cache not found"))
        .when(_systemServices)
        .clearSpecificCache("missing");
    mockMvc
        .perform(delete("/api/system/cache/clear/missing").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void getCacheList_path() throws Exception {
    mockMvc
        .perform(get("/api/system/cache/list").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    when(_systemServices.getCacheList()).thenThrow(new IllegalStateException("err"));
    mockMvc
        .perform(get("/api/system/cache/list").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }
}
