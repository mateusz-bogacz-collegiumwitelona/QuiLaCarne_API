package com.example.restaurant.dto.domain;

public record UserDomain(
        String token,
        String username,
        String normalizedUsername,
        String email,
        String normalizedEmail
) {
}
