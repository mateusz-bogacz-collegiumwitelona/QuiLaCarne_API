package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.response.TableListResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface ITableRespository {
    public List<TableListResponse> findAllTables(String lang, OffsetDateTime startTime, OffsetDateTime endTime);
}
