package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.helpers.ResultHandler;

import java.util.Optional;

public interface IUserServices {
    String create(RegisterRequest request, String userRole, boolean isActive);

    void activeUser(String userToken);

    Optional<UserDomain> findMinimalByEmail(String email);

    void changePassword(String token, String newPassword);

    ResultHandler<Void> updatePassword(String userToken, UpdatePasswordRequest request);

    ResultHandler<Void> updateEmail(String userToken, String email);

    ResultHandler<Void> confirmEmailChange(String userToken, String token);

    ResultHandler<Void> updateUserName(String userName, String userToken);

    ResultHandler<Void> deleteAccount(String userToken);
}
