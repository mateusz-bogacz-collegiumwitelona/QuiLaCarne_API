package com.example.restaurant.helpers;

import com.example.restaurant.enums.WebSocketEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketEvent<T> {
    private WebSocketEventType eventType;
    private String entityType;
    private String token;
    private T payload;

    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);

    public static <T> WebSocketEvent<T> created(String entityType, String token, T payload) {
        return WebSocketEvent.<T>builder()
                .eventType(WebSocketEventType.CREATED)
                .entityType(entityType)
                .token(token)
                .payload(payload)
                .build();
    }

    public static <T> WebSocketEvent<T> updated(String entityType, String token, T payload) {
        return WebSocketEvent.<T>builder()
                .eventType(WebSocketEventType.UPDATED)
                .entityType(entityType)
                .token(token)
                .payload(payload)
                .build();
    }

    public static WebSocketEvent<Void> deleted(String entityType, String token) {
        return WebSocketEvent.<Void>builder()
                .eventType(WebSocketEventType.DELETED)
                .entityType(entityType)
                .token(token)
                .payload(null)
                .build();
    }
}
