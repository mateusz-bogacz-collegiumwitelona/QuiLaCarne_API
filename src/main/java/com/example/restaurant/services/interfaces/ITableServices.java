package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;

import java.util.List;

public interface ITableServices {
    public ResultHandler<List<TableListResponse>> getTables();
}
