package com.example.restaurant.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TwoFactorServicesTest {

  @InjectMocks private TwoFactorServices twoFactorServices;

  @Mock private GoogleAuthenticator googleAuthenticator;

  @Test
  @DisplayName("Should generate correct QR Code URI")
  void shouldGenerateCorrectQrUri() {
    String secret = "JBSWY3DPEHPK3PXP";
    String username = "admin";

    String result = twoFactorServices.generateQrCodeImageUri(secret, username);

    assertTrue(result.contains("otpauth://totp/QuiLaCarne:admin"));
    assertTrue(result.contains("secret=JBSWY3DPEHPK3PXP"));
    assertTrue(result.contains("issuer=QuiLaCarne"));
  }

  @Test
  @DisplayName("Should return true when OTP code is valid")
  void shouldReturnTrueWhenOtpIsValid() {
    String secret = "SECRET";
    int code = 123456;

    when(googleAuthenticator.authorize(secret, code)).thenReturn(true);

    boolean isValid = twoFactorServices.isOptValid(secret, code);

    assertTrue(isValid);
  }
}
