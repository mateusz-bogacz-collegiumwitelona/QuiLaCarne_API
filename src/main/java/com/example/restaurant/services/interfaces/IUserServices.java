package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.helpers.ResultHandler;

public interface IUserServices {
    ResultHandler<Void> updatePassword(String userToken, UpdatePasswordRequest request);

    ResultHandler<Void> updateEmail(String userToken, String email);

    ResultHandler<Void> confirmEmailChange(String userToken, String token);

    ResultHandler<Void> updateUserName(String userName, String userToken);

    ResultHandler<Void> deleteAccount(String userToken);
}
