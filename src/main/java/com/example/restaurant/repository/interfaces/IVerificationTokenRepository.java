package com.example.restaurant.repository.interfaces;

import com.example.restaurant.enums.TokenTypeEnum;

public interface IVerificationTokenRepository {
    String createToken(String userToken, TokenTypeEnum type, int expiryMinutes);

    boolean activeUser(String token);

    boolean resetUserPassowrd(String token, String newPassword);

    boolean validateToken(String userToken, String tokenValue, TokenTypeEnum type);
}
