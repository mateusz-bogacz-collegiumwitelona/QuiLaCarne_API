package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.GoogleLoginRequest;
import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.ResetPasswordRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.helpers.ResultHandler;

public interface IAuthServices {
    ResultHandler<AuthResponse> authenticate(LoginRequest request);

    ResultHandler<Void> register(RegisterRequest request);

    ResultHandler<Boolean> registerConfirm(String token);

    ResultHandler<Void> resetPassword(String email);

    ResultHandler<Boolean> setNewPassword(ResetPasswordRequest request);

    ResultHandler<AuthResponse> authenticateWithGoogle(GoogleLoginRequest request);
}
