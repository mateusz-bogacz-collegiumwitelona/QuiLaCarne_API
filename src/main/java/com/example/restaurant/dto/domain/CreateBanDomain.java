package com.example.restaurant.dto.domain;

import com.example.restaurant.models.Users;

import java.time.OffsetDateTime;

public record CreateBanDomain(
        Users client,
        Users admin,
        String reason,
        OffsetDateTime expiresAt
) {
}
