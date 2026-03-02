package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IAuthServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthServices _authServices;

    @PostMapping("/login")
    public ResponseEntity<ResultHandler<AuthResponse>> login(@RequestBody LoginRequest request) {
        ResultHandler<AuthResponse> result = _authServices.authenticate(request);

        return ResponseEntity
                .status(result.getStatusCode())
                .body(result);
    }
}
