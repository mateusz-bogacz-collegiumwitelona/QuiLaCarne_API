package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.TableListResponse;

import java.util.List;

public interface ITableServices {
    List<TableListResponse> getTables(TableFilterRequest request);

    void changeStatusToClean(String tableToken);

    void changeStatusToOutOfService(String tableToken);
}
