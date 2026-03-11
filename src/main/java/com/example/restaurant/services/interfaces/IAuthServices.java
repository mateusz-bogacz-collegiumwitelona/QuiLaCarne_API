package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.ResetPasswordRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.helpers.ResultHandler;

public interface IAuthServices {
    ResultHandler<AuthResponse> authenticate(LoginRequest request);
    ResultHandler<String> register(RegisterRequest request);
    ResultHandler<String> registerConfirm(String token);
    ResultHandler<String> resetPassowrd(String email);
    ResultHandler<String> setNewPassword(ResetPasswordRequest request);
}
