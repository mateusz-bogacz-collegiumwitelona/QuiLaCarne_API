package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.dto.response.Verify2faLoginRequest;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.exceptions.GoogleAuthenticationException;
import com.example.restaurant.exceptions.InvalidDateException;
import com.example.restaurant.fasade.interfaces.IUserFacade;
import com.example.restaurant.helpers.staics.RoleType;
import com.example.restaurant.models.Users;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IAuthServices;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServices implements IAuthServices {
  private final AuthenticationManager _authManager;
  private final JwtServices _jwtServices;
  private final EmailServices _emailServices;
  private final UserDetailsService _userDetailsService;
  private final IUserFacade _userServices;
  private final IVerificationTokenServices _tokenServices;
  private final IUserRepository _userRepo;
  private final TwoFactorServices _2faServices;

  @Value("${application.security.google.client-id}")
  private String googleClientId;

  @Override
  @Auditable(action = "USER_LOGIN")
  public AuthResponse authenticate(LoginRequest request) {
    var auth =
        _authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    Users user = (Users) auth.getPrincipal();

    if (user.getIsTwoFactorEnabled() != null && user.getIsTwoFactorEnabled()) {
      String preAuthToken =
          _tokenServices.createToken(user.getToken(), TokenTypeEnum.PRE_AUTH_2FA, 5);

      return AuthResponse.builder()
          .token(preAuthToken)
          .username(user.getUsername())
          .requires2fa(true)
          .build();
    }

    return buildSuccessAuthResponse(user);
  }

  @Auditable(action = "USER_LOGOUT")
  @Transactional
  @Override
  public void logout(String userToken) {
    _tokenServices.revokeTokensForUser(userToken, TokenTypeEnum.REFRESH_TOKEN);
  }

  @Auditable(action = "USER_REGISTERED")
  @Transactional
  public void register(RegisterRequest request) {

    if (!request.getPassword().equals(request.getConfirmPassword()))
      throw new IllegalStateException("Passwords do not match");

    String userToken = _userServices.create(request, RoleType.ROLE_CLIENT, false);

    String activationToken =
        _tokenServices.createToken(userToken, TokenTypeEnum.ACTIVATION, 24 * 60);

    _emailServices.sendActivationEmail(request.getEmail(), request.getUsername(), activationToken);
    log.info("Register new user {}", request.getUsername());
  }

  @Override
  @Auditable(action = "REGISTER_CONFIRM")
  @Transactional
  public Boolean registerConfirm(String token) {
    var userTokenOpt = _tokenServices.validateToken(token, TokenTypeEnum.ACTIVATION);

    if (userTokenOpt.isEmpty()) throw new InvalidDateException("Invalid or expired token");

    _userServices.activeUser(userTokenOpt.get());

    log.info("User {} confirmed registration", userTokenOpt.get());

    return true;
  }

  @Override
  @Auditable(action = "RESET_PASSWORD")
  @Transactional
  public void resetPassword(String email) {
    var userOpt = _userServices.findMinimalByEmail(email);

    if (userOpt.isPresent()) {
      UserDomain userMiniml = userOpt.get();

      String resetToken =
          _tokenServices.createToken(userMiniml.token(), TokenTypeEnum.PASSWORD_RESET, 15);

      _emailServices.sendResetPasswordEmail(userMiniml.email(), userMiniml.username(), resetToken);
      log.info("Reset password for user {}", userMiniml.username());
    }
  }

  @Override
  @Auditable(action = "SET_NEW_PASSWORD")
  @Transactional
  public Boolean setNewPassword(ResetPasswordRequest request) {
    if (!request.getPassword().equals(request.getConfirmPassword()))
      throw new IllegalStateException("Passwords do not match");

    var userTokenOpt =
        _tokenServices.validateToken(request.getToken(), TokenTypeEnum.PASSWORD_RESET);

    if (userTokenOpt.isEmpty()) throw new IllegalStateException("Invalid or expired token");

    _userServices.changePassword(userTokenOpt.get(), request.getConfirmPassword());

    log.info("Change password for user {}", userTokenOpt.get());

    return true;
  }

  @Auditable(action = "USER_GOOGLE_LOGIN")
  @Transactional
  @Override
  public AuthResponse authenticateWithGoogle(GoogleLoginRequest request) {
    try {
      GoogleIdToken.Payload payload = verifyGoogleToken(request.getToken());

      if (payload == null) throw new BadCredentialsException("Invalid ID token");

      String email = payload.getEmail();
      var userOpt = _userServices.findMinimalByEmail(email);

      String usernameToLogin;

      if (userOpt.isEmpty()) {
        usernameToLogin = _userServices.createOAuthUser(email);
      } else {
        usernameToLogin = userOpt.get().username();
      }

      UserDetails userDetails = _userDetailsService.loadUserByUsername(usernameToLogin);
      return buildSuccessAuthResponse(userDetails);

    } catch (Exception ex) {
      log.error("Google authentication error", ex);
      throw new GoogleAuthenticationException("Authentication failed", ex);
    }
  }

  @Auditable(action = "USER_LOGIN_2FA")
  @Transactional
  @Override
  public AuthResponse verify2faLogin(Verify2faLoginRequest request) {
    var userTokenOpt =
        _tokenServices.validateToken(request.getPreAuthToken(), TokenTypeEnum.PRE_AUTH_2FA);

    if (userTokenOpt.isEmpty())
      throw new BadCredentialsException("Pre-Auth token is invalid or expired");

    Users user = _userRepo.findByToken(userTokenOpt.get());

    boolean isValid = _2faServices.isOptValid(user.getMfaSecret(), request.getCode());

    if (!isValid) throw new BadCredentialsException("Invalid 2FA code");

    return buildSuccessAuthResponse(user);
  }

  @Auditable(action = "USER_REFRESH_TOKEN")
  @Transactional
  @Override
  public AuthResponse refreshToken(RefreshTokenRequest request) {
    var userTokenOpt =
        _tokenServices.validateToken(request.getRefreshToken(), TokenTypeEnum.REFRESH_TOKEN);

    if (userTokenOpt.isEmpty())
      throw new BadCredentialsException("Refresh token is invalid or expired");

    Users user = _userRepo.findByToken(userTokenOpt.get());
    UserDetails userDetails = _userDetailsService.loadUserByUsername(user.getUsername());

    return buildSuccessAuthResponse(userDetails);
  }

  private AuthResponse buildSuccessAuthResponse(UserDetails userDetails) {
    if (!userDetails.isEnabled()) throw new BadCredentialsException("User not enabled");

    String jwtToken = _jwtServices.generateToken(userDetails);

    if (jwtToken == null) throw new BadCredentialsException("Jwt Token not generated");

    Users user =
        _userRepo
            .findByNormalizedUsername(userDetails.getUsername().trim().toUpperCase())
            .orElseThrow(() -> new BadCredentialsException("User not found"));

    _tokenServices.revokeTokensForUser(user.getToken(), TokenTypeEnum.REFRESH_TOKEN);

    String refreshToken =
        _tokenServices.createToken(
            user.getToken(), TokenTypeEnum.REFRESH_TOKEN, 60 * 24 * 7 // 7 days
            );

    return AuthResponse.builder()
        .token(jwtToken)
        .refreshToken(refreshToken)
        .username(userDetails.getUsername())
        .requires2fa(false)
        .build();
  }

  private GoogleIdToken.Payload verifyGoogleToken(String token) {
    try {
      GoogleIdTokenVerifier verifier =
          new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
              .setAudience(Collections.singletonList(googleClientId))
              .build();

      GoogleIdToken idToken = verifier.verify(token);
      return idToken != null ? idToken.getPayload() : null;

    } catch (GeneralSecurityException | IOException ex) {
      throw new GoogleAuthenticationException("Failed to verify Google token", ex);
    }
  }
}
