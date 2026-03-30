package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.UpdatePasswordRequest;

import java.util.Optional;

public interface IUserServices {
    String create(RegisterRequest request, String userRole, boolean isActive);

    void activeUser(String userToken);

    Optional<UserDomain> findMinimalByEmail(String email);

    void changePassword(String token, String newPassword);

    void updatePassword(String userToken, UpdatePasswordRequest request);

    void updateEmail(String userToken, String email);

    void confirmEmailChange(String userToken, String token);

    void updateUserName(String userName, String userToken);

    void deleteAccount(String userToken);
}
