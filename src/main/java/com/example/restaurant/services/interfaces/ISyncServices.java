package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.response.*;
import com.example.restaurant.helpers.PagedResult;

import java.util.List;

public interface ISyncServices {
    SyncBootstrapResponse getBootstrapManifest();

    SyncDictionariesResponse getDictionaries();

    List<SyncRoleResponse> getRoles();

    PagedResult<SyncDishResponse> getDishesSync(int page);

    PagedResult<SyncBanResponse> getBansSync(int page);

    PagedResult<SyncReportResponse> getReportsSync(int page);

    PagedResult<SyncIngredientResponse> getIngredientsSync(int page);

    PagedResult<SyncOrderResponse> getOrdersSync(int page);

    PagedResult<SyncOrderItemResponse> getOrderItemsSync(int page);

    PagedResult<SyncReservationResponse> getReservationsSync(int page);

    PagedResult<SyncTableResponse> getTablesSync(int page);

    PagedResult<SyncUserResponse> getUsersSync(int page);
}
