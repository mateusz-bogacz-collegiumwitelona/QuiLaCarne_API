package com.example.restaurant.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.config.RateLimitConfig;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.exceptions.FileProcessingException;
import com.example.restaurant.fasade.interfaces.IDishFacade;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = DishController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class DishControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private IDishFacade _dishServices;

  @Test
  void getMenu_path() throws Exception {
    mockMvc
        .perform(
            get("/api/dishes").with(auth()).with(csrf()).param("page", "1").param("size", "10"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/dishes").with(auth()).with(csrf()).param("page", "0").param("size", "10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removeDish_path() throws Exception {
    mockMvc
        .perform(delete("/api/dishes/{token}", "DISH_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing")).when(_dishServices).remove("MISSING");
    mockMvc
        .perform(delete("/api/dishes/{token}", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void changeAvailable_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/dishes")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\"DISH_1\",\"available\":false,\"unavailableReason\":\"Brak skladnikow\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/dishes")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"\",\"available\":false}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void editDish_path() throws Exception {
    MockMultipartFile editPhoto =
        new MockMultipartFile("photo", "dish.jpg", "image/jpeg", "x".getBytes());
    mockMvc
        .perform(
            multipart("/api/dishes")
                .file(editPhoto)
                .param("dishToken", "DISH_1")
                .param("newName", "Nowa nazwa")
                .param("price", "42")
                .with(
                    request -> {
                      request.setMethod("PUT");
                      return request;
                    })
                .with(auth())
                .with(csrf()))
        .andExpect(status().isOk());

    doThrow(new FileProcessingException("error", new RuntimeException()))
        .when(_dishServices)
        .edit(any());
    mockMvc
        .perform(
            multipart("/api/dishes")
                .file(editPhoto)
                .param("dishToken", "DISH_1")
                .with(
                    request -> {
                      request.setMethod("PUT");
                      return request;
                    })
                .with(auth())
                .with(csrf()))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void addDish_path() throws Exception {
    MockMultipartFile addPhoto =
        new MockMultipartFile("photo", "dish2.jpg", "image/jpeg", "x".getBytes());
    mockMvc
        .perform(
            multipart("/api/dishes")
                .file(addPhoto)
                .param("name", "Carbonara")
                .param("price", "30")
                .param("categoryToken", "CAT_1")
                .param("ingredientTokens", "ING_1")
                .with(auth())
                .with(csrf()))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            multipart("/api/dishes")
                .file(addPhoto)
                .param("name", "")
                .param("price", "30")
                .param("categoryToken", "CAT_1")
                .with(auth())
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getDictionary_path() throws Exception {
    mockMvc
        .perform(get("/api/dishes/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    when(_dishServices.getDictionary()).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/dishes/dictionary").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addCategory_path() throws Exception {
    mockMvc
        .perform(
            post("/api/dishes/category/add")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"Przystawki\",\"nameEn\":\"Starters\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/dishes/category/add")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namePl\":\"\",\"nameEn\":\"Starters\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removeCategory_path() throws Exception {
    mockMvc
        .perform(delete("/api/dishes/category/{token}", "CAT_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing")).when(_dishServices).removeCategory("MISSING");
    mockMvc
        .perform(delete("/api/dishes/category/{token}", "MISSING").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void getPublicMenu_path() throws Exception {
    mockMvc
        .perform(get("/api/dishes/menu/public").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    when(_dishServices.getPublicMenu()).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(get("/api/dishes/menu/public").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }
}
