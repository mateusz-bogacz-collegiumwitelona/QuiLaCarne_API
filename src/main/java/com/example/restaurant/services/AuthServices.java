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
import com.example.restaurant.services.interfaces.IAuthServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServices implements IAuthServices {
    private final AuthenticationManager _authManager;
    private final JwtServices _jwtServices;
    private final IUserRepository _userRepository;
    private final IRoleRepository _roleRepository;
    private final EmailServices _emailServices;
    private final IVerificationTokenRepository _verificationTokenRepository;

    public ResultHandler<AuthResponse> authenticate(LoginRequest request) {
        try {
            var auth = _authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();

            if (!userDetails.isEnabled())
                return ResultHandler.failure("User no enabled", HttpStatus.UNAUTHORIZED.value());

            String jwtToken = _jwtServices.generateToken(userDetails);

            if (jwtToken == null)
                return ResultHandler.failure("Jwt Token no generate", HttpStatus.INTERNAL_SERVER_ERROR.value());

            AuthResponse response = AuthResponse.builder()
                    .token(jwtToken)
                    .username(userDetails.getUsername())
                    .build();

            if (response == null) return ResultHandler.failure(
                    "Server error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );

            return ResultHandler.success(
                    "Login successfull",
                    HttpStatus.OK.value(),
                    response);
        } catch (AuthenticationException aex) {
            return ResultHandler.failure(
                    "Invalid credentials",
                    HttpStatus.UNAUTHORIZED.value(),
                    List.of(aex.getMessage())
            );
        } catch (Exception ex) {
            return ResultHandler.failure(
                    "Server error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    List.of(ex.getMessage())
            );
        }
    }

    @Transactional
    public ResultHandler<String> register(RegisterRequest request) {
        try {
            if (_userRepository.existsByUsername(request.getUsername()))
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

            String result = _userRepository.createUser(request, role, false);

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
        } catch (Exception ex) {
            return ResultHandler.failure(
                    "Server error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    List.of(ex.getMessage())
            );
        }


    }

    @Transactional
    public ResultHandler<String> registerConfirm(String token) {
        try {
            boolean isSuccess = _verificationTokenRepository.activeUser(token);

            if (!isSuccess)
                return ResultHandler.failure("Invalid token", HttpStatus.BAD_REQUEST.value());

            return ResultHandler.success(
                    "User activated successfully",
                    HttpStatus.OK.value());
        } catch (Exception ex) {
            return ResultHandler.failure(
                    "Server error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    List.of(ex.getMessage())
            );
        }
    }

    @Transactional
    public ResultHandler<String> resetPassowrd(String email) {
        try {
            var userOpt = _userRepository.findMinimalByEmail(email);

            if (userOpt.isPresent()) {
                UserMinimalDTO userMiniml = userOpt.get();

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
        } catch (Exception ex) {
            return ResultHandler.failure(
                    "Server error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    List.of(ex.getMessage()));
        }
    }

    @Transactional
    public ResultHandler<String> setNewPassword(ResetPasswordRequest request) {
        try {
            if (!request.getPassword().equals(request.getConfirmPassword()))
                return ResultHandler.failure(
                        "Passwords do not match",
                        HttpStatus.BAD_REQUEST.value()
                );

            boolean isSuccess = _verificationTokenRepository.resetUserPassowrd(
                    request.getToken(),
                    request.getConfirmPassword()
            );

            if (!isSuccess)
                return ResultHandler.failure("Invalid token", HttpStatus.BAD_REQUEST.value());

            return ResultHandler.success(
                    "Reset password successfully",
                    HttpStatus.OK.value());
        } catch (Exception ex) {
            return ResultHandler.failure(
                    "Server error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    List.of(ex.getMessage())
            );
        }
    }
}
