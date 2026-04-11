package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.response.SyncBootstrapResponse;
import com.example.restaurant.dto.response.SyncDictionariesResponse;

public interface ISyncServices {
    SyncBootstrapResponse getBootstrapManifest();

    SyncDictionariesResponse getDictionaries();
}
