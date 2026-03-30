package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.ResetPasswordRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.InvalidDateException;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IUserServices;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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
    private IUserRepository _userRepository;
    @Mock
    private EmailServices _emailServices;
    @Mock
    private IUserServices _userServices;
    @Mock
    private IVerificationTokenServices _tokenServices;
    @Mock
    private Authentication _auth;
    @Mock
    private UserDetails _userDetails;

    private RegisterRequest _registerRequest;

    @BeforeEach
    void setUp() {
        _registerRequest = new RegisterRequest();
        _registerRequest.setUsername(TestConstants.FAKE_USERNAME);
        _registerRequest.setEmail(TestConstants.FAKE_EMAIL);
        _registerRequest.setPassword(TestConstants.FAKE_PASSWORD);
        _registerRequest.setConfirmPassword(TestConstants.FAKE_PASSWORD);
    }

    @Test
    @DisplayName("Authenticate: Success")
    void authenticate_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TestConstants.FAKE_USERNAME);
        loginRequest.setPassword(TestConstants.FAKE_PASSWORD);

        when(_authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(_auth);
        when(_auth.getPrincipal()).thenReturn(_userDetails);
        when(_userDetails.isEnabled()).thenReturn(true);
        when(_userDetails.getUsername()).thenReturn(TestConstants.FAKE_USERNAME);
        when(_jwtServices.generateToken(any(UserDetails.class))).thenReturn("fake-jwt-token");

        AuthResponse result = _authServices.authenticate(loginRequest);

        assertNotNull(result);
        assertEquals("fake-jwt-token", result.getToken());
        assertEquals(TestConstants.FAKE_USERNAME, result.getUsername());
    }

    @Test
    @DisplayName("Authenticate: Throws Exception when user is disabled")
    void authenticate_ShouldThrowException_WhenUserIsDisabled() {
        LoginRequest loginRequest = new LoginRequest();
        when(_authManager.authenticate(any())).thenReturn(_auth);
        when(_auth.getPrincipal()).thenReturn(_userDetails);
        when(_userDetails.isEnabled()).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> _authServices.authenticate(loginRequest));
    }

    @Test
    @DisplayName("Register: Throws EntityAlreadyExistsException if username taken")
    void register_ShouldThrowException_WhenUsernameExists() {
        when(_userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> _authServices.register(_registerRequest));
    }

    @Test
    @DisplayName("Register: Throws IllegalStateException if passwords mismatch")
    void register_ShouldThrowException_WhenPasswordsMismatch() {
        _registerRequest.setConfirmPassword("mismatch");
        assertThrows(IllegalStateException.class, () -> _authServices.register(_registerRequest));
    }

    @Test
    @DisplayName("Register: Throws Exception when email already exist")
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        when(_userRepository.existsByUsername(anyString())).thenReturn(false);
        when(_userRepository.existByEmail(anyString())).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> _authServices.register(_registerRequest));
        verify(_userServices, never()).create(any(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Register Confirm: Success")
    void registerConfirm_ShouldReturnTrue_WhenTokenIsValid() {
        when(_tokenServices.validateToken("valid-token", TokenTypeEnum.ACTIVATION)).thenReturn(Optional.of(TestConstants.FAKE_USER_TOKEN));

        Boolean result = _authServices.registerConfirm("valid-token");

        assertTrue(result);
        verify(_userServices).activeUser(TestConstants.FAKE_USER_TOKEN);
    }

    @Test
    @DisplayName("Register: Success")
    void register_ShouldSucceed_AndCallAllDependencies() {
        when(_userRepository.existsByUsername(anyString())).thenReturn(false);
        when(_userRepository.existByEmail(anyString())).thenReturn(false);
        when(_userServices.create(any(), eq("ROLE_CLIENT"), eq(false))).thenReturn("user-token");
        when(_tokenServices.createToken(eq("user-token"), eq(TokenTypeEnum.ACTIVATION), anyInt())).thenReturn("act-token");

        _authServices.register(_registerRequest);

        verify(_emailServices).sendActivationEmail(TestConstants.FAKE_EMAIL, TestConstants.FAKE_USERNAME, "act-token");
    }

    @Test
    @DisplayName("Register Confirm: Throws InvalidDateException when token invalid")
    void registerConfirm_ShouldThrowException_WhenTokenInvalid() {
        when(_tokenServices.validateToken(anyString(), any())).thenReturn(Optional.empty());

        assertThrows(InvalidDateException.class, () -> _authServices.registerConfirm("invalid-token"));
    }

    @Test
    @DisplayName("Set New Password: Throws IllegalStateException if passwords mismatch")
    void setNewPassword_ShouldThrowException_WhenPasswordsMismatch() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword("pass");
        request.setConfirmPassword("different");

        assertThrows(IllegalStateException.class, () -> _authServices.setNewPassword(request));
    }

    @Test
    @DisplayName("Set New Password: Successes if token is valid")
    void setNewPassword_ShouldSucceed_WhenTokenIsValid() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("valid-token");
        req.setPassword("NewPass123!");
        req.setConfirmPassword("NewPass123!");

        when(_tokenServices.validateToken("valid-token", TokenTypeEnum.PASSWORD_RESET)).thenReturn(Optional.of("user-token"));

        _authServices.setNewPassword(req);

        verify(_userServices).changePassword("user-token", "NewPass123!");
    }

    @Test
    @DisplayName("Reset Password: Do nothing when user not found")
    void resetPassword_ShouldDoNothing_WhenUserNotFound() {
        when(_userServices.findMinimalByEmail(anyString())).thenReturn(Optional.empty());

        _authServices.resetPassword("nonexistent@test.pl");

        verify(_tokenServices, never()).createToken(anyString(), any(), anyInt());
        verify(_emailServices, never()).sendResetPasswordEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Reset Password: Send email when user  found")
    void resetPassword_ShouldSendEmail_WhenUserFound() {
        UserDomain domain = new UserDomain("token", "user", "USER", "test@test.pl", "TEST@TEST.PL");
        when(_userServices.findMinimalByEmail(anyString())).thenReturn(Optional.of(domain));
        when(_tokenServices.createToken(anyString(), eq(TokenTypeEnum.PASSWORD_RESET), anyInt())).thenReturn("reset-token");

        _authServices.resetPassword("test@test.pl");

        verify(_emailServices).sendResetPasswordEmail("test@test.pl", "user", "reset-token");
    }
}