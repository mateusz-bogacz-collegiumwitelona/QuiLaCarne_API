package com.example.restaurant.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RefreshTokenRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.ResetPasswordRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.dto.response.UserProfileResponse;
import com.example.restaurant.dto.response.Verify2faLoginRequest;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.exceptions.InvalidDateException;
import com.example.restaurant.exceptions.UserBlockedException;
import com.example.restaurant.fasade.interfaces.IUserFacade;
import com.example.restaurant.models.Users;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class AuthServicesTest {

  @InjectMocks private AuthServices _authServices;

  @Mock private AuthenticationManager _authManager;

  @Mock private JwtServices _jwtServices;

  @Mock private EmailServices _emailServices;

  @Mock private IUserFacade _userServices;

  @Mock private IVerificationTokenServices _tokenServices;

  @Mock private Authentication _auth;

  @Mock private TwoFactorServices _2faServices;

  @Mock private IUserRepository _userRepo;

  @Mock private Users _user;

  @Mock private UserDetailsService _userDetailsServices;

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
  @DisplayName("Authenticate: Success (2FA Disabled)")
  void authenticate_ShouldReturnAuthResponse_WhenCredentialsAreValid_And_2FA_Disabled() {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername(TestConstants.FAKE_USERNAME);
    loginRequest.setPassword(TestConstants.FAKE_PASSWORD);

    when(_authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(_auth);
    when(_auth.getPrincipal()).thenReturn(_user);
    when(_user.getIsTwoFactorEnabled()).thenReturn(false);
    when(_user.isEnabled()).thenReturn(true);
    when(_user.getUsername()).thenReturn(TestConstants.FAKE_USERNAME);

    doReturn(List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))).when(_user).getAuthorities();

    when(_jwtServices.generateToken(any(UserDetails.class)))
        .thenReturn(TestConstants.FAKE_ACTION_TOKEN);

    when(_userRepo.findByNormalizedUsername(anyString())).thenReturn(Optional.of(_user));
    when(_tokenServices.createToken(any(), eq(TokenTypeEnum.REFRESH_TOKEN), anyInt()))
        .thenReturn("fake-refresh-token");

    AuthResponse result = _authServices.authenticate(loginRequest);

    assertNotNull(result);
    assertFalse(result.isRequires2fa());
    assertEquals(TestConstants.FAKE_ACTION_TOKEN, result.getToken());
    assertEquals("fake-refresh-token", result.getRefreshToken());
    assertEquals(TestConstants.FAKE_USERNAME, result.getUsername());
    assertTrue(result.getRoles().contains("ROLE_CLIENT"));
  }

  @Test
  @DisplayName("Authenticate: Pre-Auth Token (2FA Enabled)")
  void authenticate_ShouldReturnPreAuthToken_WhenCredentialsAreValid_And_2FA_Enabled() {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername(TestConstants.FAKE_USERNAME);
    loginRequest.setPassword(TestConstants.FAKE_PASSWORD);

    String preAuthToken = "fake-pre-auth-token";

    when(_authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(_auth);
    when(_auth.getPrincipal()).thenReturn(_user);
    when(_user.getIsTwoFactorEnabled()).thenReturn(true);
    when(_user.getToken()).thenReturn(TestConstants.FAKE_USER_TOKEN);
    when(_user.getUsername()).thenReturn(TestConstants.FAKE_USERNAME);
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))).when(_user).getAuthorities();
    when(_tokenServices.createToken(TestConstants.FAKE_USER_TOKEN, TokenTypeEnum.PRE_AUTH_2FA, 5))
        .thenReturn(preAuthToken);

    AuthResponse result = _authServices.authenticate(loginRequest);

    assertNotNull(result);
    assertTrue(result.isRequires2fa());
    assertEquals(preAuthToken, result.getToken());
    assertEquals(TestConstants.FAKE_USERNAME, result.getUsername());
    assertTrue(result.getRoles().contains("ROLE_MANAGER"));
    verify(_jwtServices, never()).generateToken(any());
  }

  @Test
  @DisplayName("Authenticate: Throws Exception when user is disabled")
  void authenticate_ShouldThrowException_WhenUserIsDisabled() {
    LoginRequest loginRequest = new LoginRequest();
    when(_authManager.authenticate(any())).thenReturn(_auth);
    when(_auth.getPrincipal()).thenReturn(_user);
    when(_user.getIsTwoFactorEnabled()).thenReturn(false);
    when(_user.isEnabled()).thenReturn(false);

    assertThrows(UserBlockedException.class, () -> _authServices.authenticate(loginRequest));
  }

  @Test
  @DisplayName("Logout: Success")
  void logout_ShouldRevokeRefreshToken() {
    _authServices.logout(TestConstants.FAKE_USER_TOKEN);

    verify(_tokenServices, times(1))
        .revokeTokensForUser(TestConstants.FAKE_USER_TOKEN, TokenTypeEnum.REFRESH_TOKEN);
  }

  @Test
  @DisplayName("Verify 2FA Login: Success")
  void verify2faLogin_ShouldReturnJwtToken_WhenCodeIsValid() {
    Verify2faLoginRequest request = new Verify2faLoginRequest();
    request.setPreAuthToken("valid-pre-auth-token");
    request.setCode(123456);

    String fakeSecret = "SUPER_SECRET_KEY";

    when(_tokenServices.validateToken("valid-pre-auth-token", TokenTypeEnum.PRE_AUTH_2FA))
        .thenReturn(Optional.of(TestConstants.FAKE_USER_TOKEN));
    when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(_user);
    when(_user.getMfaSecret()).thenReturn(fakeSecret);
    when(_2faServices.isOptValid(fakeSecret, 123456)).thenReturn(true);
    when(_user.isEnabled()).thenReturn(true);
    when(_user.getUsername()).thenReturn(TestConstants.FAKE_USERNAME);
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))).when(_user).getAuthorities();
    when(_jwtServices.generateToken(_user)).thenReturn(TestConstants.FAKE_ACTION_TOKEN);

    when(_userRepo.findByNormalizedUsername(anyString())).thenReturn(Optional.of(_user));
    when(_tokenServices.createToken(any(), eq(TokenTypeEnum.REFRESH_TOKEN), anyInt()))
        .thenReturn("fake-refresh-token");

    AuthResponse result = _authServices.verify2faLogin(request);

    assertNotNull(result);
    assertFalse(result.isRequires2fa());
    assertEquals(TestConstants.FAKE_ACTION_TOKEN, result.getToken());
    assertEquals("fake-refresh-token", result.getRefreshToken());
    assertEquals(TestConstants.FAKE_USERNAME, result.getUsername());
  }

  @Test
  @DisplayName("Verify 2FA Login: Throws Exception when Token is invalid")
  void verify2faLogin_ShouldThrowException_WhenTokenIsInvalid() {
    Verify2faLoginRequest request = new Verify2faLoginRequest();
    request.setPreAuthToken("invalid-pre-auth-token");

    when(_tokenServices.validateToken("invalid-pre-auth-token", TokenTypeEnum.PRE_AUTH_2FA))
        .thenReturn(Optional.empty());

    assertThrows(AuthenticationException.class, () -> _authServices.verify2faLogin(request));
  }

  @Test
  @DisplayName("Verify 2FA Login: Throws Exception when Code is invalid")
  void verify2faLogin_ShouldThrowException_WhenCodeIsInvalid() {
    Verify2faLoginRequest request = new Verify2faLoginRequest();
    request.setPreAuthToken("valid-pre-auth-token");
    request.setCode(999999);

    String fakeSecret = "SUPER_SECRET_KEY";

    when(_tokenServices.validateToken("valid-pre-auth-token", TokenTypeEnum.PRE_AUTH_2FA))
        .thenReturn(Optional.of(TestConstants.FAKE_USER_TOKEN));
    when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(_user);
    when(_user.getMfaSecret()).thenReturn(fakeSecret);
    when(_2faServices.isOptValid(fakeSecret, 999999)).thenReturn(false);

    assertThrows(BadCredentialsException.class, () -> _authServices.verify2faLogin(request));
    verify(_jwtServices, never()).generateToken(any());
  }

  @Test
  @DisplayName("Register: Throws IllegalStateException if passwords mismatch")
  void register_ShouldThrowException_WhenPasswordsMismatch() {
    _registerRequest.setConfirmPassword(TestConstants.FAKE_ACTION);
    assertThrows(IllegalStateException.class, () -> _authServices.register(_registerRequest));
  }

  @Test
  @DisplayName("Register Confirm: Success")
  void registerConfirm_ShouldReturnTrue_WhenTokenIsValid() {
    when(_tokenServices.validateToken(TestConstants.FAKE_ACTION_TOKEN, TokenTypeEnum.ACTIVATION))
        .thenReturn(Optional.of(TestConstants.FAKE_USER_TOKEN));

    Boolean result = _authServices.registerConfirm(TestConstants.FAKE_ACTION_TOKEN);

    assertTrue(result);
    verify(_userServices).activeUser(TestConstants.FAKE_USER_TOKEN);
  }

  @Test
  @DisplayName("Register: Success")
  void register_ShouldSucceed_AndCallAllDependencies() {
    when(_userServices.create(any(), eq(TestConstants.ROLE_CLIENT), eq(false)))
        .thenReturn(TestConstants.FAKE_USER_TOKEN);
    when(_tokenServices.createToken(
            eq(TestConstants.FAKE_USER_TOKEN), eq(TokenTypeEnum.ACTIVATION), anyInt()))
        .thenReturn(TestConstants.FAKE_ACTION_TOKEN);

    _authServices.register(_registerRequest);

    verify(_emailServices)
        .sendActivationEmail(
            TestConstants.FAKE_EMAIL, TestConstants.FAKE_USERNAME, TestConstants.FAKE_ACTION_TOKEN);
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
    request.setPassword(TestConstants.FAKE_PASSWORD);
    request.setConfirmPassword(TestConstants.FAKE_DIFF_PASSWORD);

    assertThrows(IllegalStateException.class, () -> _authServices.setNewPassword(request));
  }

  @Test
  @DisplayName("Set New Password: Change password when token is valid")
  void setNewPassword_ShouldChangePassword_WhenTokenIsValid() {
    ResetPasswordRequest req = new ResetPasswordRequest();
    req.setToken(TestConstants.FAKE_VERIFICATION_TOKEN);
    req.setPassword(TestConstants.VALID_PASSWORD);
    req.setConfirmPassword(TestConstants.VALID_PASSWORD);

    when(_tokenServices.validateToken(
            TestConstants.FAKE_VERIFICATION_TOKEN, TokenTypeEnum.PASSWORD_RESET))
        .thenReturn(Optional.of(TestConstants.FAKE_USER_TOKEN));

    _authServices.setNewPassword(req);

    verify(_userServices)
        .changePassword(TestConstants.FAKE_USER_TOKEN, TestConstants.VALID_PASSWORD);
  }

  @Test
  @DisplayName("Reset Password: Do nothing when user not found")
  void resetPassword_ShouldDoNothing_WhenUserNotFound() {
    when(_userServices.findMinimalByEmail(anyString())).thenReturn(Optional.empty());

    _authServices.resetPassword(TestConstants.NON_EXISTENT_EMAIL);

    verify(_tokenServices, never()).createToken(anyString(), any(), anyInt());
    verify(_emailServices, never()).sendResetPasswordEmail(any(), any(), any());
  }

  @Test
  @DisplayName("Reset Password: Send email when user  found")
  void resetPassword_ShouldSendEmail_WhenUserFound() {
    UserDomain domain =
        new UserDomain(
            TestConstants.FAKE_USER_TOKEN,
            TestConstants.FAKE_USERNAME,
            TestConstants.ROLE_CLIENT,
            TestConstants.FAKE_EMAIL,
            TestConstants.FAKE_EMAIL.toUpperCase());

    when(_userServices.findMinimalByEmail(anyString())).thenReturn(Optional.of(domain));

    when(_tokenServices.createToken(anyString(), eq(TokenTypeEnum.PASSWORD_RESET), anyInt()))
        .thenReturn(TestConstants.FAKE_VERIFICATION_TOKEN);

    _authServices.resetPassword(TestConstants.FAKE_EMAIL);

    verify(_emailServices)
        .sendResetPasswordEmail(
            TestConstants.FAKE_EMAIL,
            TestConstants.FAKE_USERNAME,
            TestConstants.FAKE_VERIFICATION_TOKEN);
  }

  @Test
  @DisplayName("Refresh Token: Success")
  void refreshToken_Success() {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken("valid-refresh-token");

    Users mockUser = mock(Users.class);
    when(mockUser.getUsername()).thenReturn("client");
    when(mockUser.getToken()).thenReturn("user-token-123");
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))).when(mockUser).getAuthorities();

    UserDetails mockUserDetails = mock(UserDetails.class);
    when(mockUserDetails.getUsername()).thenReturn("client");
    when(mockUserDetails.isEnabled()).thenReturn(true);

    when(_tokenServices.validateToken("valid-refresh-token", TokenTypeEnum.REFRESH_TOKEN))
        .thenReturn(Optional.of("user-token-123"));

    when(_userRepo.findByToken("user-token-123")).thenReturn(mockUser);
    when(_userDetailsServices.loadUserByUsername("client")).thenReturn(mockUserDetails);

    when(_jwtServices.generateToken(mockUserDetails)).thenReturn("new-jwt-token");
    when(_userRepo.findByNormalizedUsername(anyString())).thenReturn(Optional.of(mockUser));
    when(_tokenServices.createToken(anyString(), eq(TokenTypeEnum.REFRESH_TOKEN), anyInt()))
        .thenReturn("new-refresh-token");

    AuthResponse response = _authServices.refreshToken(request);

    assertNotNull(response);
    assertEquals("new-jwt-token", response.getToken());
    assertEquals("new-refresh-token", response.getRefreshToken());
    assertEquals("client", response.getUsername());
    assertFalse(response.isRequires2fa());

    verify(_tokenServices, times(1))
        .revokeTokensForUser("user-token-123", TokenTypeEnum.REFRESH_TOKEN);
  }

  @Test
  @DisplayName("Get Current User Profile: Success")
  void getCurrentUserProfile_ShouldReturnProfile_WhenUserExists() {
    String token = "valid-user-token";

    when(_userRepo.findByToken(token)).thenReturn(_user);
    when(_user.getUsername()).thenReturn("client_user");
    when(_user.getEmail()).thenReturn("client@test.com");
    when(_user.getIsTwoFactorEnabled()).thenReturn(true);
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))).when(_user).getAuthorities();

    UserProfileResponse response = _authServices.getCurrentUserProfile(token);

    assertNotNull(response);
    assertEquals("client_user", response.getUsername());
    assertEquals("client@test.com", response.getEmail());
    assertTrue(response.is2FaEnable());
    assertTrue(response.getRoles().contains("ROLE_CLIENT"));
  }

  @Test
  @DisplayName("Get Current User Profile: Throws Exception when User not found")
  void getCurrentUserProfile_ShouldThrowException_WhenUserNotFound() {
    String invalidToken = "invalid-token";
    when(_userRepo.findByToken(invalidToken)).thenReturn(null);

    assertThrows(
        BadCredentialsException.class, () -> _authServices.getCurrentUserProfile(invalidToken));
  }
}
