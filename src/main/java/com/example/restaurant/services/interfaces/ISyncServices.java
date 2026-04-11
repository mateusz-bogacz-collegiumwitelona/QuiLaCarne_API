package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.SyncRoleResponse;
import com.example.restaurant.dto.response.SyncBootstrapResponse;
import com.example.restaurant.dto.response.SyncDictionariesResponse;

import java.util.List;

public interface ISyncServices {
    SyncBootstrapResponse getBootstrapManifest();

    SyncDictionariesResponse getDictionaries();

    List<SyncRoleResponse> getRoles();
}
