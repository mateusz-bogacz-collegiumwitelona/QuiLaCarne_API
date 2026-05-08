package com.example.restaurant.services.interfaces;

import com.example.restaurant.enums.TokenTypeEnum;
import java.util.Optional;

public interface IVerificationTokenServices {
  String createToken(String userToken, TokenTypeEnum type, int expiryMinutes);

  boolean validateToken(String userToken, String tokenValue, TokenTypeEnum type);

  Optional<String> validateToken(String tokenValue, TokenTypeEnum type);

  void revokeTokensForUser(String userToken, TokenTypeEnum type);
}
