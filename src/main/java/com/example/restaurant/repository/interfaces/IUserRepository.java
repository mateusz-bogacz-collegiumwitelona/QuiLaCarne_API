package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.domain.UserMinimalDTO;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.models.Users;

import java.util.Optional;

public interface IUserRepository {
    public String createUser(RegisterRequest request, String userRole, boolean isActive);
    public boolean existsByUsername(String username);
    public boolean changePassword(String token, String newPassword);
    public Optional<UserMinimalDTO> findMinimalByEmail(String email);
}
