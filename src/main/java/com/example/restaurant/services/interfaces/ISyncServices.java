package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.SyncRoleResponse;
import com.example.restaurant.dto.response.SyncBanResponse;
import com.example.restaurant.dto.response.SyncBootstrapResponse;
import com.example.restaurant.dto.response.SyncDictionariesResponse;
import com.example.restaurant.dto.response.SyncDishResponse;
import com.example.restaurant.helpers.PagedResult;

import java.util.List;

public interface ISyncServices {
    SyncBootstrapResponse getBootstrapManifest();

    SyncDictionariesResponse getDictionaries();

    List<SyncRoleResponse> getRoles();

    PagedResult<SyncDishResponse> getDishesSync(int page);

    PagedResult<SyncBanResponse> getBansSync(int page);
}
