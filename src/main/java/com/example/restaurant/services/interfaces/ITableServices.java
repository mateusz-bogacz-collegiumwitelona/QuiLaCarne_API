package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;

import java.time.OffsetDateTime;
import java.util.List;

public interface ITableServices {
    ResultHandler<List<TableListResponse>> getTables(OffsetDateTime startTime, OffsetDateTime endTime);
}
