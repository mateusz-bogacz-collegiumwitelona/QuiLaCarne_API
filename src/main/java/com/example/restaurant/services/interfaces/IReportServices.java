package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.ChangeReportStatusRequest;
import com.example.restaurant.dto.request.ReportFilterRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.dto.response.ReportListResponse;
import com.example.restaurant.helpers.PagedResult;

import java.util.List;

public interface IReportServices {
    void add(String waiterToken, AddReportRequest request);

    PagedResult<ReportListResponse> list(ReportFilterRequest request);

    void changeStatus(String adminToken, ChangeReportStatusRequest request);

    List<EntityResponse> getDictionary();
}
