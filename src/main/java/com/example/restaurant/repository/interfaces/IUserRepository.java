package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.RegisterRequest;

import java.util.Optional;

public interface IUserRepository {
    String createUser(RegisterRequest request, String userRole, boolean isActive);

    boolean existsByUsername(String username);

    boolean changePassword(String token, String newPassword);

    Optional<UserDomain> findMinimalByEmail(String email);

    boolean updatePassword(String userToken, String oldPassword, String newPassword);

    boolean updateEmail(String userToken, String email);

    boolean confirmEmailChange(String userToken);

    boolean activeUser(String userToken);

    boolean changeUserName(String userToken, String userName);

    boolean delete(String userToken);

    boolean isInRole(String roleToken, String userToken);
}
