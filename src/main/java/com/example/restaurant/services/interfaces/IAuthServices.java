package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.helpers.ResultHandler;

public interface IAuthServices {
    public ResultHandler<AuthResponse> authenticate(LoginRequest request);
}
