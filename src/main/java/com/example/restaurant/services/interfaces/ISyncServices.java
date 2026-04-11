package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.response.SyncBootstrapResponse;

public interface ISyncServices {
    SyncBootstrapResponse getBootstrapManifest();
}
