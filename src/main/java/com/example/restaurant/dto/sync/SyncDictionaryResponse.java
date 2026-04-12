package com.example.restaurant.dto.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncDictionaryResponse {
    private String token;
    private String nameEn;
    private String namePl;
}
