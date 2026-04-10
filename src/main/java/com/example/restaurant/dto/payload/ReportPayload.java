package com.example.restaurant.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportPayload {
    private String token;
    private String guestToken;
    private String reporterToken;
    private String reason;
    private String statusToken;
    private OffsetDateTime createdAt;
}
