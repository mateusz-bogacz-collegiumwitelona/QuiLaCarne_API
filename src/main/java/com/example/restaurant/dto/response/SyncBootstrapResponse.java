package com.example.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
public class SyncBootstrapResponse {
    private Map<String, EntityMetadata> modules;
    private OffsetDateTime serverTime;

    @Data
    @AllArgsConstructor
    public static class EntityMetadata {
        private long totalCount;
        private long totalPages;
    }
}
