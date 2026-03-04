package com.example.restaurant.dto.domain;

public record UserMinimalDTO(
        String token,
        String username,
        String email
) {}
