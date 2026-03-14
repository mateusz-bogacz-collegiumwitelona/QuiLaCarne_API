package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.helpers.ResultHandler;

public interface IUserServices {
    ResultHandler<String> updatePassword(String userToken, UpdatePasswordRequest request);
}
