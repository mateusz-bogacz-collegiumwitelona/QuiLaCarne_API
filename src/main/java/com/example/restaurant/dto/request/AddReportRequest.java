package com.example.restaurant.dto.request;

import lombok.Data;

@Data
public class AddReportRequest {
    private String clientToken;
    private String reason;
}
