package com.example.restaurant.services;

import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.helpers.ResultHandler;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServicesTest {
    @Mock
    private AuthenticationManager _authManager;

    @Mock
    private JwtServices _jwtServices;

    @InjectMocks
    private AuthServices _authServices;

    @Mock
    private UserDetails _userDetails;

    @Mock
    private Authentication _auth;

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
}