package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.dto.response.Verify2faLoginRequest;

public interface IAuthServices {
    AuthResponse authenticate(LoginRequest request);

    void register(RegisterRequest request);

    Boolean registerConfirm(String token);

    void resetPassword(String email);

    Boolean setNewPassword(ResetPasswordRequest request);

    AuthResponse authenticateWithGoogle(GoogleLoginRequest request);

    AuthResponse verify2faLogin(Verify2faLoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);
}
