package com.example.restaurant.controllers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.config.RateLimitConfig;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.services.interfaces.IUserServices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = UserController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class UserControllerWebMvcTest extends AbstractControllerWebMvcTest {

  @MockitoBean private IUserServices _userServices;

  @Test
  void updatePassword_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/user/me/password")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"oldPassword\":\"Old123!\",\"password\":\"New123!\",\"confirmPassword\":\"New123!\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/user/me/password")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"oldPassword\":\"Old123!\",\"password\":\"\",\"confirmPassword\":\"New123!\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateEmail_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/user/me/email/update")
                .with(auth())
                .with(csrf())
                .param("email", "new@mail.com"))
        .andExpect(status().isOk());

    mockMvc
        .perform(patch("/api/user/me/email/update").with(auth()).with(csrf()).param("email", ""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void confirmEmail_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/user/me/email/confirm")
                .with(auth())
                .with(csrf())
                .param("verificationToken", "TOKEN_1"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/user/me/email/confirm")
                .with(auth())
                .with(csrf())
                .param("verificationToken", ""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateUserName_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/user/me/username").with(auth()).with(csrf()).param("userName", "new_name"))
        .andExpect(status().isOk());

    mockMvc
        .perform(patch("/api/user/me/username").with(auth()).with(csrf()).param("userName", ""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteUser_path() throws Exception {
    mockMvc
        .perform(delete("/api/user/me/delete").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing")).when(_userServices).delete(any());
    mockMvc
        .perform(delete("/api/user/me/delete").with(auth()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void createEmployee_path() throws Exception {
    mockMvc
        .perform(
            post("/api/user/employee")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"register\":{\"username\":\"worker\",\"email\":\"worker@mail.com\",\"password\":\"Admin123!\",\"confirmPassword\":\"Admin123!\"},\"admin\":false}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/user/employee")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"register\":{\"username\":\"\",\"email\":\"worker@mail.com\",\"password\":\"Admin123!\",\"confirmPassword\":\"Admin123!\"},\"admin\":false}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void editEmployee_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/user/employee")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"employeeToken\":\"EMP_1\",\"email\":\"emp@mail.com\",\"userName\":\"emp\"}"))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing")).when(_userServices).editEmployee(any());
    mockMvc
        .perform(
            patch("/api/user/employee")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"employeeToken\":\"EMP_1\",\"email\":\"emp@mail.com\",\"userName\":\"emp\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void changeEmployeePassword_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/user/employee/change-password")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"employeeToken\":\"EMP_1\",\"password\":\"Admin123!\",\"confirmPassword\":\"Admin123!\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/user/employee/change-password")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"employeeToken\":\"EMP_1\",\"password\":\"\",\"confirmPassword\":\"Admin123!\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changeEmployeeRole_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/user/employee/change-role")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeToken\":\"EMP_1\",\"admin\":true}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/user/employee/change-role")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeToken\":\"\",\"admin\":true}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void blockEmployee_path() throws Exception {
    mockMvc
        .perform(
            patch("/api/user/employee/change-availability")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeToken\":\"EMP_1\",\"available\":false}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/user/employee/change-availability")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeToken\":\"\",\"available\":false}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteEmployee_path() throws Exception {
    mockMvc
        .perform(
            delete("/api/user/employee/{employeeToken}/delete", "EMP_1").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    doThrow(new EntityNotFoundException("missing"))
        .when(_userServices)
        .deleteEmployee(any(), eq("MISSING"));

    mockMvc
        .perform(
            delete("/api/user/employee/{employeeToken}/delete", "MISSING")
                .with(auth())
                .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void generate2fa_path() throws Exception {
    mockMvc
        .perform(post("/api/user/2fa/generate").with(auth()).with(csrf()))
        .andExpect(status().isOk());

    when(_userServices.generate2fa(any())).thenThrow(new IllegalStateException("bad"));
    mockMvc
        .perform(post("/api/user/2fa/generate").with(auth()).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void enable2fa_path() throws Exception {
    mockMvc
        .perform(
            post("/api/user/2fa/enable")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":123456}"))
        .andExpect(status().isOk());

    doThrow(new IllegalStateException("bad")).when(_userServices).verifyAndEnable2fa(any(), any());
    mockMvc
        .perform(
            post("/api/user/2fa/enable")
                .with(auth())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":123456}"))
        .andExpect(status().isBadRequest());
  }
}
