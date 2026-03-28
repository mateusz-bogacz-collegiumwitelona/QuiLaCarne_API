package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.GoogleLoginRequest;
import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.ResetPasswordRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import com.example.restaurant.services.interfaces.IAuthServices;
import com.example.restaurant.services.interfaces.IUserServices;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServices implements IAuthServices {
    private final AuthenticationManager _authManager;
    private final JwtServices _jwtServices;
    private final IUserRepository _userRepository;
    private final IRoleRepository _roleRepository;
    private final EmailServices _emailServices;
    private final IVerificationTokenRepository _verificationTokenRepository;
    private final UserDetailsService _userDetailsService;
    private final IUserServices _userServices;

    @Value("${application.security.google.client-id}")
    private String googleClientId;

    @Auditable(action = "USER_LOGIN")
    public ResultHandler<AuthResponse> authenticate(LoginRequest request) {
        var auth = _authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();

        return buildSuccessAuthResponse(userDetails, "Login successful");
    }

    @Auditable(action = "USER_REGISTERED")
    @Transactional
    public ResultHandler<Void> register(RegisterRequest request) {
        if (_userRepository.existsByUsername(request.getUsername().toUpperCase().trim()))
            return ResultHandler.failure(
                    "Username already exists",
                    HttpStatus.BAD_REQUEST.value()
            );

        if (!request.getPassword().equals(request.getConfirmPassword()))
            return ResultHandler.failure(
                    "Passwords do not match",
                    HttpStatus.BAD_REQUEST.value()
            );

        String role = "ROLE_CLIENT";

        if (!_roleRepository.isRoleExists(role))
            return ResultHandler.failure(
                    "Role does not exist",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );

        String result = _userServices.create(request, role, false);

        if (result == null)
            return ResultHandler.failure(
                    "User already exists",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );

        String activationToken = _verificationTokenRepository.createToken(result, TokenTypeEnum.ACTIVATION, 24 * 60);

        if (activationToken == null)
            return ResultHandler.failure(
                    "Activate token not Create",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );


        _emailServices.sendActivationEmail(request.getEmail(), request.getUsername(), activationToken);

        return ResultHandler.success(
                "User registered successfully",
                HttpStatus.CREATED.value());
    }

    @Auditable(action = "REGISTER_CONFIRM")
    @Transactional
    public ResultHandler<Boolean> registerConfirm(String token) {
        var userTokenOpt = _verificationTokenRepository.validateToken(token, TokenTypeEnum.ACTIVATION);

        if (userTokenOpt.isEmpty())
            return ResultHandler.failure(
                    "Invalid or expired token",
                    HttpStatus.BAD_REQUEST.value()
            );

        _userServices.activeUser(userTokenOpt.get());

        return ResultHandler.success(
                "User activated successfully",
                HttpStatus.OK.value(),
                true
        );
    }

    @Auditable(action = "RESET_PASSWORD")
    @Transactional
    public ResultHandler<Void> resetPassword(String email) {
        var userOpt = _userServices.findMinimalByEmail(email);

        if (userOpt.isPresent()) {
            UserDomain userMiniml = userOpt.get();

            String resetToken = _verificationTokenRepository.createToken(
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

        return ResultHandler.success(
                "If account exists, a link was sent.",
                HttpStatus.OK.value()
        );
    }

    @Auditable(action = "SET_NEW_PASSWORD")
    @Transactional
    public ResultHandler<Boolean> setNewPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword()))
            return ResultHandler.failure(
                    "Passwords do not match",
                    HttpStatus.BAD_REQUEST.value()
            );

        var userTokenOpt = _verificationTokenRepository.validateToken(request.getToken(), TokenTypeEnum.PASSWORD_RESET);

        if (userTokenOpt.isEmpty())
            return ResultHandler.failure(
                    "Invalid or expired token",
                    HttpStatus.BAD_REQUEST.value()
            );

        _userServices.changePassword(
                userTokenOpt.get(),
                request.getConfirmPassword()
        );

        return ResultHandler.success(
                "Reset password successfully",
                HttpStatus.OK.value(),
                true
        );
    }

    @Auditable(action = "USER_GOOGLE_LOGIN")
    @Transactional
    @Override
    public ResultHandler<AuthResponse> authenticateWithGoogle(GoogleLoginRequest request) {
        try {
            GoogleIdToken.Payload payload = verifyGoogleToken(request.getToken());

            if (payload == null)
                return ResultHandler.failure(
                        "Invalid ID token",
                        HttpStatus.UNAUTHORIZED.value()
                );

            String email = payload.getEmail();
            var userOpt = _userServices.findMinimalByEmail(email);

            String usernameToLogin;

            if (userOpt.isEmpty()) {
                usernameToLogin = registerWithGoogle(payload);
            } else {
                usernameToLogin = userOpt.get().username();
            }

            UserDetails userDetails = _userDetailsService.loadUserByUsername(usernameToLogin);
            return buildSuccessAuthResponse(userDetails, "Google login successful");

        } catch (Exception ex) {
            log.error("Google authentication error", ex);
            return ResultHandler.failure("Authentication failed", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private ResultHandler<AuthResponse> buildSuccessAuthResponse(UserDetails userDetails, String message) {
        if (!userDetails.isEnabled())
            return ResultHandler.failure(
                    "User not enabled",
                    HttpStatus.UNAUTHORIZED.value()
            );

        String jwtToken = _jwtServices.generateToken(userDetails);

        if (jwtToken == null)
            return ResultHandler.failure(
                    "Jwt Token not generated",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );

        AuthResponse response = AuthResponse.builder()
                .token(jwtToken)
                .username(userDetails.getUsername())
                .build();

        return ResultHandler.success(
                message,
                HttpStatus.OK.value(),
                response
        );
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

    private String registerWithGoogle(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        String baseUserName = email.split("@")[0];
        int counter = 1;

        while (_userRepository.existsByUsername(baseUserName.toUpperCase().trim())) {
            baseUserName = baseUserName + counter;
            counter++;
        }

        String randomPassword = UUID.randomUUID().toString() + "G00G1E#";

        RegisterRequest register = new RegisterRequest();
        register.setEmail(email);
        register.setPassword(randomPassword);
        register.setUsername(baseUserName);
        register.setConfirmPassword(randomPassword);

        _userServices.create(register, "ROLE_CLIENT", true);

        return baseUserName;
    }
}
