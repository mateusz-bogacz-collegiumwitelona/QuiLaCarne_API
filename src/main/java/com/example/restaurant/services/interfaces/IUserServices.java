package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.helpers.ResultHandler;

public interface IUserServices {
    ResultHandler<Boolean> updatePassword(String userToken, UpdatePasswordRequest request);

    ResultHandler<Void> updateEmail(String userToken, String email);

    ResultHandler<Boolean> confirmEmailChange(String userToken, String token);

    ResultHandler<Boolean> updateUserName(String userName, String userToken);

    ResultHandler<Boolean> deleteAccount(String userToken);
}
