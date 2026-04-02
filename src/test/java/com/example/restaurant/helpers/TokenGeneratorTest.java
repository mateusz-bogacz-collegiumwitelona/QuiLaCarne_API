package com.example.restaurant.helpers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TokenGeneratorTest {
    @Test
    @DisplayName("generateToken: Should generate a non-null and non-empty string")
    void generateToken_ShouldReturnValidString() {
        String token = TokenGenerator.generateToken();
        assertNotNull(token);
    }

    @Test
    @DisplayName("generateToken: Should generate unique tokens")
    void generateToken_ShouldGenerateUniqueValues() {
        String token1 = TokenGenerator.generateToken();
        String token2 = TokenGenerator.generateToken();

        assertNotEquals(token1, token2);
    }
}
