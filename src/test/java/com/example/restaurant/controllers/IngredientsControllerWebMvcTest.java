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
import com.example.restaurant.services.interfaces.IIngredientsServices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = IngredientsController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class IngredientsControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private IIngredientsServices _ingredientsServices;

  @Test
  void addIngredient_path() throws Exception {
    mockMvc
        .perform(
            post("/api/ingredients")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"entity\":{\"namePl\":\"Pomidor\",\"nameEn\":\"Tomato\"},\"allergenTokens\":[]}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/ingredients")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"entity\":{\"namePl\":\"\",\"nameEn\":\"Tomato\"},\"allergenTokens\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removeIngredient_path() throws Exception {
    mockMvc
        .perform(delete("/api/ingredients/{token}", "ING_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing")).when(_ingredientsServices).remove("MISSING");
    mockMvc
        .perform(delete("/api/ingredients/{token}", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void getDictionary_path() throws Exception {
    mockMvc
        .perform(get("/api/ingredients/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    when(_ingredientsServices.getDictionary()).thenThrow(new IllegalStateException("boom"));
    mockMvc
        .perform(get("/api/ingredients/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }
}
