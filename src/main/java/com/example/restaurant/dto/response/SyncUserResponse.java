package com.example.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncUserResponse {
    private String token;
    private String username;
    private String email;
    private Boolean isActive;
    private boolean isStaff;
    private List<String> roleTokens;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}