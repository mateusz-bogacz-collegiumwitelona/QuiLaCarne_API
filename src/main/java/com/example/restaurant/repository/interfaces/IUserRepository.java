package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.models.Users;

import java.util.Optional;

public interface IUserRepository {
    String createUser(RegisterRequest request, String userRole, boolean isActive);

    boolean existsByUsername(String username);

    void changePassword(String token, String newPassword);

    Optional<UserDomain> findMinimalByEmail(String email);

    void updatePassword(String userToken, String oldPassword, String newPassword);

    void updateEmail(String userToken, String email);

    void confirmEmailChange(String userToken);

    void activeUser(String userToken);

    void changeUserName(String userToken, String userName);

    void delete(String userToken);

    boolean isInRole(String roleToken, String userToken);

    Users findByToken(String token);
}
