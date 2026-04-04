package com.example.restaurant.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Generate2faResponse {
    private String qrCodeUri;
    private String manualCode;
}
