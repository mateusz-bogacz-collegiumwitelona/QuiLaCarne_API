package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.models.RestaurantTables;

import java.time.OffsetDateTime;
import java.util.List;

public interface ITableRespository {
    List<TableListResponse> findAllTables(String lang, OffsetDateTime startTime, OffsetDateTime endTime);

    boolean isTableExist(String token);

    void changeStatus(String token, String statusToken);

    RestaurantTables findByToken(String token);
}
