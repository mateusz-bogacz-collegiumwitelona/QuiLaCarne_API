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
public class OrderPayload {
    private String token;
    private Integer totalPrice;
    private String reservationToken;
    private String tableToken;
    private String waiterToken;
    private List<String> statusTokens;
    private List<OrderItemPayload> items;
}
