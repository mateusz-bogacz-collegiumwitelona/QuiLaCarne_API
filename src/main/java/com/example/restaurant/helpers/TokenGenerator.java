package com.example.restaurant.helpers;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenGenerator {
    private static final SecureRandom rand = new SecureRandom();

    public static String generateToken() {
        byte[] tokenBytes = new byte[64];
        rand.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
