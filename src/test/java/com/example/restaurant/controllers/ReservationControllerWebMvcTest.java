package com.example.restaurant.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.example.restaurant.fasade.interfaces.IReservationFacade;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = ReservationController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class ReservationControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private IReservationFacade _reservationServices;

  @Test
  void create_path() throws Exception {
    String validRequest =
            "{\"tableToken\":\"TABLE_1\",\"startTime\":\""
                    + OffsetDateTime.now().plusHours(2)
                    + "\",\"endTime\":\""
                    + OffsetDateTime.now().plusHours(3)
                    + "\",\"dishes\":[{\"dishToken\":\"DISH_1\",\"quantity\":1}]}";

    mockMvc
            .perform(
                    post("/api/reservations")
                            .with(auth())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequest))
            .andExpect(status().isOk());

    String emptyDishesRequest =
            "{\"tableToken\":\"TABLE_1\",\"startTime\":\""
                    + OffsetDateTime.now().plusHours(2)
                    + "\",\"endTime\":\""
                    + OffsetDateTime.now().plusHours(3)
                    + "\",\"dishes\":[]}";

    mockMvc
            .perform(
                    post("/api/reservations")
                            .with(auth())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyDishesRequest))
            .andExpect(status().isBadRequest());

    doThrow(new IllegalStateException("bad")).when(_reservationServices).create(any(), anyString());
    mockMvc
            .perform(
                    post("/api/reservations")
                            .with(auth())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequest))
            .andExpect(status().isBadRequest());
  }

  @Test
  void list_path() throws Exception {
    mockMvc
        .perform(
            get("/api/reservations")
                .with(auth())
                .with(csrf())
                .param("page", "1")
                .param("size", "10"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/reservations")
                .with(auth())
                .with(csrf())
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void detail_path() throws Exception {
    mockMvc
        .perform(get("/api/reservations/{token}", "RES_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    when(_reservationServices.details("MISSING", "user-token"))
        .thenThrow(new EntityNotFoundException("missing"));
    mockMvc
        .perform(get("/api/reservations/{token}", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void cancel_path() throws Exception {
    mockMvc
        .perform(patch("/api/reservations/{token}/cancel", "RES_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing"))
        .when(_reservationServices)
        .cancel("MISSING", "user-token");
    mockMvc
        .perform(patch("/api/reservations/{token}/cancel", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void removeItem_path() throws Exception {
    mockMvc
        .perform(
            delete("/api/reservations/item/remove")
                .with(auth())
                .with(csrf())
                .param("reservationToken", "RES_1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishToken\":\"DISH_1\",\"quantity\":1,\"note\":\"bez cebuli\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            delete("/api/reservations/item/remove")
                .with(auth())
                .with(csrf())
                .param("reservationToken", "RES_1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishToken\":\"\",\"quantity\":1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addItem_path() throws Exception {
    mockMvc
        .perform(
            post("/api/reservations/item/add")
                .with(auth())
                .with(csrf())
                .param("reservationToken", "RES_1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"dishToken\":\"DISH_1\",\"quantity\":1,\"note\":\"bez soli\"}]"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/reservations/item/add")
                .with(auth())
                .with(csrf())
                .param("reservationToken", "RES_1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"dishToken\":\"\",\"quantity\":1}]"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void assignWaiter_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/reservations/{token}/assign-waiter", "RES_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new IllegalStateException("bad"))
        .when(_reservationServices)
        .assignWaiter("RES_1", "user-token");
    mockMvc
        .perform(
            patch("/api/reservations/{token}/assign-waiter", "RES_1").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void absent_path() throws Exception {
    mockMvc
        .perform(patch("/api/reservations/{token}/absent", "RES_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new IllegalStateException("bad")).when(_reservationServices).isAbsent("RES_1");
    mockMvc
        .perform(patch("/api/reservations/{token}/absent", "RES_1").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getDictionary_path() throws Exception {
    mockMvc
        .perform(get("/api/reservations/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    when(_reservationServices.getDictionary()).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/reservations/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void complete_path() throws Exception {
    mockMvc
        .perform(patch("/api/reservations/{token}/complete", "RES_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new IllegalStateException("bad")).when(_reservationServices).markAsComplete("RES_1");
    mockMvc
        .perform(patch("/api/reservations/{token}/complete", "RES_1").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }
}
