package com.example.restaurant.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class UserListResponse {
    private String token;
    private String username;
    private String email;
    private Boolean isActive;
    private List<String> roles;
    private OffsetDateTime createdAt;
}