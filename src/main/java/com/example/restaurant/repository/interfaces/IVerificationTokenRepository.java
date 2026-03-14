package com.example.restaurant.repository.interfaces;

import com.example.restaurant.enums.TokenTypeEnum;

import java.util.Optional;

public interface IVerificationTokenRepository {
    String createToken(String userToken, TokenTypeEnum type, int expiryMinutes);
    
    boolean validateToken(String userToken, String tokenValue, TokenTypeEnum type);

    Optional<String> validateToken(String tokenValue, TokenTypeEnum type);
}
