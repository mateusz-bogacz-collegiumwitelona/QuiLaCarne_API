package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.GoogleLoginRequest;
import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.ResetPasswordRequest;
import com.example.restaurant.dto.response.AuthResponse;

public interface IAuthServices {
    AuthResponse authenticate(LoginRequest request);

    void register(RegisterRequest request);

    Boolean registerConfirm(String token);

    void resetPassword(String email);

    Boolean setNewPassword(ResetPasswordRequest request);

    AuthResponse authenticateWithGoogle(GoogleLoginRequest request);
}
