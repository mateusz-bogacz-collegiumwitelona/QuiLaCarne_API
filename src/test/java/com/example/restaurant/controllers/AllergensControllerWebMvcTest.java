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
import com.example.restaurant.services.interfaces.IAllergensServices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = AllergensController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class AllergensControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private IAllergensServices _allergensServices;

  @Test
  @WithMockUser
  void getAllergensDictionary_path() throws Exception {
    mockMvc.perform(get("/api/dishes/allergens/dictionary")).andExpect(status().isOk());

    when(_allergensServices.getDictionary()).thenThrow(new IllegalStateException("err"));
    mockMvc.perform(get("/api/dishes/allergens/dictionary")).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void addAllergen_path() throws Exception {
    mockMvc
        .perform(
            post("/api/dishes/allergens/add")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"Alergen\",\"nameEn\":\"Allergen\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/dishes/allergens/add")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"\",\"nameEn\":\"Allergen\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void removeAllergen_path() throws Exception {
    mockMvc
        .perform(delete("/api/dishes/allergen/{token}", "ALLERGEN_1").with(csrf()))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("not found")).when(_allergensServices).remove("MISSING");
    mockMvc
        .perform(delete("/api/dishes/allergen/{token}", "MISSING").with(csrf()))
        .andExpect(status().isNotFound());
  }
}
