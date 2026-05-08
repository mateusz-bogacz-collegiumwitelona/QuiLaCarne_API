package com.example.restaurant.services;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServicesTest {

  @InjectMocks private JwtServices _jwtServices;

  private UserDetails _userDetails;

  @BeforeEach
  void setUp() {
    String testSecret =
        Base64.getEncoder()
            .encodeToString(
                "super_secret_test_key_32_characters_long".getBytes(StandardCharsets.UTF_8));
    ReflectionTestUtils.setField(_jwtServices, "privateKey", testSecret);
    ReflectionTestUtils.setField(_jwtServices, "jwtExpiration", 3600000L);

    _userDetails = new User("testuser", "password", new ArrayList<>());
  }

  @Test
  @DisplayName("Generate & Validate Token: Should create valid token and extract username")
  void generateAndValidateToken_ShouldWork() {
    String token = _jwtServices.generateToken(_userDetails);
    assertNotNull(token);
    assertEquals("testuser", _jwtServices.extractUsername(token));
    assertTrue(_jwtServices.isTokenValid(token, _userDetails));
  }

  @Test
  @DisplayName("Token Validation: Should return false for wrong user")
  void isTokenValid_ShouldReturnFalse_ForWrongUser() {
    String token = _jwtServices.generateToken(_userDetails);
    UserDetails wrongUser = new User("otheruser", "password", new ArrayList<>());
    assertFalse(_jwtServices.isTokenValid(token, wrongUser));
  }

  @Test
  @DisplayName("Token Expiration: Should detect expired tokens")
  void isTokenExpired_ShouldDetectExpiration() {
    ReflectionTestUtils.setField(_jwtServices, "jwtExpiration", -1000L);
    String token = _jwtServices.generateToken(_userDetails);
    assertThrows(Exception.class, () -> _jwtServices.isTokenValid(token, _userDetails));
  }
}
