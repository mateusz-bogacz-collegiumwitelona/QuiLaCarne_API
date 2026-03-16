package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.helpers.ResultHandler;

public interface IUserServices {
    ResultHandler<String> updatePassword(String userToken, UpdatePasswordRequest request);

    ResultHandler<String> updateEmail(String userToken, String email);

    ResultHandler<String> confirmEmailChange(String userToken, String token);

    ResultHandler<String> updateUserName(String userName, String userToken);

    ResultHandler<String> deleteAccount(String userToken);
}
