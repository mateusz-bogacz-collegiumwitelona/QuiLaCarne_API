package com.example.restaurant.services;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TwoFactorServices {
  private final GoogleAuthenticator _gAuth;

  public String generateNewSecret() {
    GoogleAuthenticatorKey key = _gAuth.createCredentials();
    return key.getKey();
  }

  public String generateQrCodeImageUri(String secret, String username) {
    String issuer = "QuiLaCarne";

    return String.format(
        "otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, username, secret, issuer);
  }

  public boolean isOptValid(String secret, int code) {
    return _gAuth.authorize(secret, code);
  }
}
