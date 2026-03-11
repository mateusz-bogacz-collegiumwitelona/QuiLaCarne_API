package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.RegisterRequest;

import java.util.Optional;

public interface IUserRepository {
    String createUser(RegisterRequest request, String userRole, boolean isActive);
    boolean existsByUsername(String username);
    boolean changePassword(String token, String newPassword);
    Optional<UserDomain> findMinimalByEmail(String email);
}
