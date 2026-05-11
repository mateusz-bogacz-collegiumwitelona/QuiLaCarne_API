package com.example.restaurant.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.config.RateLimitConfig;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.services.interfaces.IAuthServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc
class AuthControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private IAuthServices _authServices;

  @BeforeEach
  void setup() {
    when(_authServices.authenticate(any())).thenReturn(authResponse());
    when(_authServices.authenticateWithGoogle(any())).thenReturn(authResponse());
    when(_authServices.verify2faLogin(any())).thenReturn(authResponse());
    when(_authServices.refreshToken(any())).thenReturn(authResponse());
    when(_authServices.registerConfirm(anyString())).thenReturn(true);
    when(_authServices.setNewPassword(any())).thenReturn(true);
  }

  @Test
  void login_path() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
        .andExpect(status().isOk())
        .andExpect(header().exists("Set-Cookie"));

    doThrow(new IllegalStateException("invalid")).when(_authServices).authenticate(any());
    mockMvc
        .perform(
            post("/api/auth/login")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void logout_path() throws Exception {
    mockMvc.perform(post("/api/auth/logout").with(auth()).with(csrf())).andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing")).when(_authServices).logout(any());
    mockMvc
        .perform(post("/api/auth/logout").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void register_path() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"newuser\",\"email\":\"new@user.com\",\"password\":\"Admin123!\",\"confirmPassword\":\"Admin123!\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/auth/register")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"newuser\",\"email\":\"new@user.com\",\"password\":\"\",\"confirmPassword\":\"Admin123!\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registerConfirm_path() throws Exception {
    mockMvc
        .perform(get("/api/auth/register-confirm").with(auth()).param("token", "TOKEN_1"))
        .andExpect(status().isOk());

    when(_authServices.registerConfirm(anyString()))
        .thenThrow(new IllegalStateException("expired"));
    mockMvc
        .perform(get("/api/auth/register-confirm").with(auth()).param("token", "TOKEN_1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resetPassword_path() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .with(auth())
                .with(csrf())
                .param("email", "client@test.com"))
        .andExpect(status().isOk());

    doThrow(new IllegalStateException("bad")).when(_authServices).resetPassword(anyString());
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .with(auth())
                .with(csrf())
                .param("email", "client@test.com"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void setPassword_path() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/set-password")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\"T1\",\"password\":\"Admin123!\",\"confirmPassword\":\"Admin123!\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/auth/set-password")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\"\",\"password\":\"Admin123!\",\"confirmPassword\":\"Admin123!\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void google_path() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/google")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"google-id-token\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/auth/google")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void verify2fa_path() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/verify-2fa")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preAuthToken\":\"PRE_1\",\"code\":123456}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/auth/verify-2fa")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preAuthToken\":\"\",\"code\":123456}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refresh_path() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"REFRESH_1\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/auth/refresh").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }
}
