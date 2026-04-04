package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.GoogleLoginRequest;
import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.ResetPasswordRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.dto.response.Verify2faLoginRequest;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.exceptions.InvalidDateException;
import com.example.restaurant.models.Users;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IAuthServices;
import com.example.restaurant.services.interfaces.IUserServices;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServices implements IAuthServices {
    private final AuthenticationManager _authManager;
    private final JwtServices _jwtServices;
    private final EmailServices _emailServices;
    private final UserDetailsService _userDetailsService;
    private final IUserServices _userServices;
    private final IVerificationTokenServices _tokenServices;
    private final IUserRepository _userRepo;
    private final TwoFactorServices _2faServices;

    @Value("${application.security.google.client-id}")
    private String googleClientId;


    @Auditable(action = "USER_LOGIN")
    public AuthResponse authenticate(LoginRequest request) {
        var auth = _authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        Users user = (Users) auth.getPrincipal();

        if (user.getIsTwoFactorEnabled() != null && user.getIsTwoFactorEnabled()) {
            String preAuthToken = _tokenServices.createToken(
                    user.getToken(),
                    TokenTypeEnum.PRE_AUTH_2FA,
                    5
            );

            return AuthResponse.builder()
                    .token(preAuthToken)
                    .username(user.getUsername())
                    .requires2fa(true)
                    .build();
        }

        return buildSuccessAuthResponse(user);
    }

    @Auditable(action = "USER_REGISTERED")
    @Transactional
    public void register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword()))
            throw new IllegalStateException("Passwords do not match");


        String ROLE_CLIENT = "ROLE_CLIENT";
        String userToken = _userServices.create(request, ROLE_CLIENT, false);

        String activationToken = _tokenServices.createToken(userToken, TokenTypeEnum.ACTIVATION, 24 * 60);

        _emailServices.sendActivationEmail(request.getEmail(), request.getUsername(), activationToken);

    }

    @Auditable(action = "REGISTER_CONFIRM")
    @Transactional
    public Boolean registerConfirm(String token) {
        var userTokenOpt = _tokenServices.validateToken(token, TokenTypeEnum.ACTIVATION);

        if (userTokenOpt.isEmpty())
            throw new InvalidDateException("Invalid or expired token");

        _userServices.activeUser(userTokenOpt.get());

        return true;
    }

    @Auditable(action = "RESET_PASSWORD")
    @Transactional
    public void resetPassword(String email) {
        var userOpt = _userServices.findMinimalByEmail(email);

        if (userOpt.isPresent()) {
            UserDomain userMiniml = userOpt.get();

            String resetToken = _tokenServices.createToken(
                    userMiniml.token(),
                    TokenTypeEnum.PASSWORD_RESET,
                    15
            );

            _emailServices.sendResetPasswordEmail(
                    userMiniml.email(),
                    userMiniml.username(),
                    resetToken
            );
        }
    }

    @Auditable(action = "SET_NEW_PASSWORD")
    @Transactional
    public Boolean setNewPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword()))
            throw new IllegalStateException("Passwords do not match");

        var userTokenOpt = _tokenServices.validateToken(request.getToken(), TokenTypeEnum.PASSWORD_RESET);

        if (userTokenOpt.isEmpty())
            throw new IllegalStateException("Invalid or expired token");

        _userServices.changePassword(
                userTokenOpt.get(),
                request.getConfirmPassword()
        );

        return true;
    }

    @Auditable(action = "USER_GOOGLE_LOGIN")
    @Transactional
    @Override
    public AuthResponse authenticateWithGoogle(GoogleLoginRequest request) {
        try {
            GoogleIdToken.Payload payload = verifyGoogleToken(request.getToken());

            if (payload == null)
                throw new AuthenticationException("Invalid ID token") {
                    @Override
                    public String getMessage() {
                        return super.getMessage();
                    }
                };

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
            throw new RuntimeException("Authentication failed");
        }
    }

    @Auditable(action = "USER_LOGIN_2FA")
    @Transactional
    @Override
    public AuthResponse verify2faLogin(Verify2faLoginRequest request) {
        var userTokenOpt = _tokenServices.validateToken(request.getPreAuthToken(), TokenTypeEnum.PRE_AUTH_2FA);

        if (userTokenOpt.isEmpty()) {
            throw new AuthenticationException("Pre-Auth token is invalid or expired") {
                @Override
                public String getMessage() {
                    return super.getMessage();
                }
            };
        }

        Users user = _userRepo.findByToken(userTokenOpt.get());

        boolean isValid = _2faServices.isOptValid(user.getMfaSecret(), request.getCode());

        if (!isValid) throw new BadCredentialsException("Invalid 2FA code");

        return buildSuccessAuthResponse(user);
    }

    private AuthResponse buildSuccessAuthResponse(UserDetails userDetails) {
        if (!userDetails.isEnabled())
            throw new AuthenticationException("User not enabled") {
                @Override
                public String getMessage() {
                    return super.getMessage();
                }
            };


        String jwtToken = _jwtServices.generateToken(userDetails);

        if (jwtToken == null)
            throw new RuntimeException("Jwt Token not generated");

        return AuthResponse.builder()
                .token(jwtToken)
                .username(userDetails.getUsername())
                .requires2fa(false)
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleToken(String token) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(token);
        return idToken != null ? idToken.getPayload() : null;
    }
}
