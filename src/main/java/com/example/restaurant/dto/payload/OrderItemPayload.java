package com.example.restaurant.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemPayload {
    private String token;
    private String dishToken;
    private Integer quantity;
    private Integer priceAtTimeOfOrder;
    private String note;
    private List<String> statusTokens;
}
