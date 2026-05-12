package com.example.restaurant.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.config.RateLimitConfig;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.services.interfaces.ITableServices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = TableController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class TableControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private ITableServices _tableServices;

  @Test
  void getTables_path() throws Exception {
    mockMvc.perform(get("/api/tables").with(auth()).with(csrf())).andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/tables")
                .with(auth())
                .with(csrf())
                .param("startTime", "2026-05-10T12:00:00Z")
                .param("endTime", "2026-05-09T12:00:00Z"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void clearTables_path() throws Exception {
    mockMvc
        .perform(patch("/api/tables/{token}/clear", "TABLE_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    doThrow(new EntityNotFoundException("missing"))
        .when(_tableServices)
        .changeStatusToClean("MISSING");
    mockMvc
        .perform(patch("/api/tables/{token}/clear", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void outOfServices_path() throws Exception {
    mockMvc
        .perform(patch("/api/tables/{token}/out-of-services", "TABLE_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    doThrow(new EntityNotFoundException("missing"))
        .when(_tableServices)
        .changeStatusToOutOfService("MISSING");
    mockMvc
        .perform(patch("/api/tables/{token}/out-of-services", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void addTable_path() throws Exception {
    mockMvc
        .perform(
            post("/api/tables")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tableNumber\":15,\"capacity\":4}"))
        .andExpect(status().isCreated());

    doThrow(new IllegalStateException("exists")).when(_tableServices).add(any());
    mockMvc
        .perform(
            post("/api/tables")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tableNumber\":15,\"capacity\":4}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteTable_path() throws Exception {
    mockMvc
        .perform(delete("/api/tables/{token}/delete", "TABLE_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    doThrow(new EntityNotFoundException("missing")).when(_tableServices).delete("MISSING");
    mockMvc
        .perform(delete("/api/tables/{token}/delete", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void getDictionary_path() throws Exception {
    mockMvc
        .perform(get("/api/tables/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    when(_tableServices.getDictionary()).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/tables/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addStatus_path() throws Exception {
    mockMvc
        .perform(
            post("/api/tables/status/add")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"Wolny\",\"nameEn\":\"Available\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/tables/status/add")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"\",\"nameEn\":\"Available\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removeStatus_path() throws Exception {
    mockMvc
        .perform(delete("/api/tables/status/{token}", "STATUS_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    doThrow(new EntityNotFoundException("missing")).when(_tableServices).removeStatus("MISSING");
    mockMvc
        .perform(delete("/api/tables/status/{token}", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void changeStatusToAvalaible_path() throws Exception {
    mockMvc
        .perform(patch("/api/tables/{token}/avalaible", "TABLE_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing"))
        .when(_tableServices)
        .changeStatusToAvalaible("MISSING");

    mockMvc
        .perform(patch("/api/tables/{token}/avalaible", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }
}
