package com.example.restaurant.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.config.RateLimitConfig;
import com.example.restaurant.services.interfaces.ISyncServices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = SyncController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class SyncControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private ISyncServices _syncServices;

  @Test
  void bootstrap_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/bootstrap").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    when(_syncServices.getBootstrapManifest()).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/bootstrap").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void dictionaries_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/dictionaries").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    when(_syncServices.getDictionaries()).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/dictionaries").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void roles_path() throws Exception {
    mockMvc.perform(get("/api/sync/roles").with(auth()).with(csrf())).andExpect(status().isOk());
    when(_syncServices.getRoles()).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/roles").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void dishes_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/dishes").with(auth()).with(csrf()).param("page", "1"))
        .andExpect(status().isOk());
    when(_syncServices.getDishesSync(-1)).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/dishes").with(auth()).with(csrf()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void bans_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/bans").with(auth()).with(csrf()).param("page", "1"))
        .andExpect(status().isOk());
    when(_syncServices.getBansSync(-1)).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/bans").with(auth()).with(csrf()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reports_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/reports").with(auth()).with(csrf()).param("page", "1"))
        .andExpect(status().isOk());
    when(_syncServices.getReportsSync(-1)).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/reports").with(auth()).with(csrf()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void ingredients_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/ingredients").with(auth()).with(csrf()).param("page", "1"))
        .andExpect(status().isOk());
    when(_syncServices.getIngredientsSync(-1)).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/ingredients").with(auth()).with(csrf()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void orders_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/orders").with(auth()).with(csrf()).param("page", "1"))
        .andExpect(status().isOk());
    when(_syncServices.getOrdersSync(-1)).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/orders").with(auth()).with(csrf()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void orderItems_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/order-items").with(auth()).with(csrf()).param("page", "1"))
        .andExpect(status().isOk());
    when(_syncServices.getOrderItemsSync(-1)).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/order-items").with(auth()).with(csrf()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reservations_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/reservations").with(auth()).with(csrf()).param("page", "1"))
        .andExpect(status().isOk());
    when(_syncServices.getReservationsSync(-1)).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/reservations").with(auth()).with(csrf()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void tables_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/tables").with(auth()).with(csrf()).param("page", "1"))
        .andExpect(status().isOk());
    when(_syncServices.getTablesSync(-1)).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/tables").with(auth()).with(csrf()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void users_path() throws Exception {
    mockMvc
        .perform(get("/api/sync/users").with(auth()).with(csrf()).param("page", "1"))
        .andExpect(status().isOk());
    when(_syncServices.getUsersSync(-1)).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/sync/users").with(auth()).with(csrf()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }
}
