package com.example.restaurant.controllers;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.config.RateLimitConfig;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.services.interfaces.IOrderServices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = OrderController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class OrderControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private IOrderServices _orderServices;

  @Test
  void getDictionary_path() throws Exception {
    mockMvc
        .perform(get("/api/order/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    when(_orderServices.getDictionary()).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/order/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getItemStatusesDictionary_path() throws Exception {
    mockMvc
        .perform(get("/api/order/item/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    when(_orderServices.getItemStatusesDictionary()).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/order/item/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addStatus_path() throws Exception {
    mockMvc
        .perform(
            post("/api/order/status/add")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"Nowy\",\"nameEn\":\"New\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/order/status/add")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"\",\"nameEn\":\"New\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addItemStatus_path() throws Exception {
    mockMvc
        .perform(
            post("/api/order/item/status/add")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"Pozycja\",\"nameEn\":\"Item\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/order/item/status/add")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"Pozycja\",\"nameEn\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removeStatus_path() throws Exception {
    mockMvc
        .perform(delete("/api/order/status/{token}", "ORDER_STATUS_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());
    doThrow(new EntityNotFoundException("missing")).when(_orderServices).removeStatus("MISSING");
    mockMvc
        .perform(delete("/api/order/status/{token}", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void removeItemStatus_path() throws Exception {
    mockMvc
        .perform(
            delete("/api/order/item/status/{token}", "ORDER_ITEM_STATUS_1")
                .with(auth())
                .with(csrf()))
        .andExpect(status().isOk());
    doThrow(new EntityNotFoundException("missing"))
        .when(_orderServices)
        .removeItemStatus("MISSING");
    mockMvc
        .perform(delete("/api/order/item/status/{token}", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }
}
