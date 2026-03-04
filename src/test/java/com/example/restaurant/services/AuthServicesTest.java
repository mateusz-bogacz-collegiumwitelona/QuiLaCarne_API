package com.example.restaurant.services;

import com.example.restaurant.dto.domain.UserMinimalDTO;
import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.ResetPasswordRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServicesTest {
    @InjectMocks
    private AuthServices _authServices;

    @Mock
    private AuthenticationManager _authManager;

    @Mock
    private JwtServices _jwtServices;

    @Mock
    private UserDetails _userDetails;

    @Mock
    private Authentication _auth;

    @Mock
    private IUserRepository _userRepository;

    @Mock
    IRoleRepository _roleRepository;

    @Mock
    private EmailServices _emailServices;

    @Mock
    private IVerificationTokenRepository _verificationTokenRepository;

    private LoginRequest _loginRequest;

    @BeforeEach
    void setUp() {
        _loginRequest = new LoginRequest();
        _loginRequest.setUsername("testuser");
        _loginRequest.setPassword("testpassword");
    }

    @Test
    void authenticate_ShouldReturnSuccess_WhenCredentialsAreValid() {
        when(_authManager.authenticate(any())).thenReturn(_auth);
        when(_auth.getPrincipal()).thenReturn(_userDetails);
        when(_userDetails.getUsername()).thenReturn("testuser");
        when(_userDetails.isEnabled()).thenReturn(true);
        when(_jwtServices.generateToken(any(UserDetails.class))).thenReturn("token");

        ResultHandler<AuthResponse> result = _authServices.authenticate(_loginRequest);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("token", result.getData().getToken());
    }

    @Test
    void authenticate_ShouldReturnFailure_WhenCredentialsAreInvalid() {
        when(_authManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        ResultHandler<AuthResponse> result = _authServices.authenticate(_loginRequest);

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getStatusCode());
    }

    @Test
    void regiser_ShouldFail_WhenPasswordIsNotMatch() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("testuser@exaple.pl");
        request.setPassword("Pass123!");
        request.setConfirmPassword("FPass123!");

        var result = _authServices.register(request);

        assertFalse(result.isSuccess());
        assertEquals("Passwords do not match", result.getMessage());
        verifyNoInteractions(_emailServices);
    }

    @Test
    void register_ShouldSuccess_AndSendEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser2");
        request.setEmail("testuser2@exaple.pl");
        request.setPassword("Pass123!");
        request.setConfirmPassword("Pass123!");

        when(_userRepository.existsByUsername("testuser2")).thenReturn(false);
        when(_roleRepository.isRoleExists("ROLE_CLIENT")).thenReturn(true);
        when(_userRepository.createUser(any(), anyString(), anyBoolean())).thenReturn("fake-user-token");

        when(_verificationTokenRepository.createToken(eq("fake-user-token"), eq(TokenTypeEnum.ACTIVATION), anyInt()))
                .thenReturn("fake-activation-token");

        var result = _authServices.register(request);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());


        verify(_emailServices).sendActivationEmail(
                eq("testuser2@exaple.pl"),
                eq("testuser2"),
                eq("fake-activation-token")
        );
    }

    @Test
    void resetPassword_ShouldSendEmail_WhenUserExists() {
        String email = "test@test.pl";
        UserMinimalDTO userDto = new UserMinimalDTO("fake-token", "testuser", email);

        when(_userRepository.findMinimalByEmail(email)).thenReturn(Optional.of(userDto));
        when(_verificationTokenRepository
                .createToken(
                        anyString(),
                        eq(TokenTypeEnum.PASSWORD_RESET),
                        anyInt()
                )
        ).thenReturn("res-token");

        var result = _authServices.resetPassowrd(email);

        assertTrue(result.isSuccess());
        verify(_emailServices).sendResetPasswordEmail(eq(email), eq("testuser"), eq("res-token"));
    }

    @Test
    void setNewPassword_ShouldReturnBadRequest_WhenPasswordIsNotMatch() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword("Pass123!");
        request.setConfirmPassword("DiffPass123!");

        var result = _authServices.setNewPassword(request);

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertEquals("Passwords do not match", result.getMessage());
    }
}