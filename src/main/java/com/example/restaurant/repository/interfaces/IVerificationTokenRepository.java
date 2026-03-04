package com.example.restaurant.repository.interfaces;

import com.example.restaurant.enums.TokenTypeEnum;

public interface IVerificationTokenRepository {
    public String createToken(String userToken, TokenTypeEnum type, int expiryMinutes);
    public boolean activeUser(String token);
}
