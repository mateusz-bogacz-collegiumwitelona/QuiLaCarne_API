package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.RegisterRequest;

public interface IUserRepository {
    public String createUser(RegisterRequest request, String userRole, boolean isActive);
    public boolean existsByUsername(String username);
}
