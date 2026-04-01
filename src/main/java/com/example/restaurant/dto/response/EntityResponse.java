package com.example.restaurant.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntityResponse {
    private String name;
    private String token;
}
