package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.ChangeReportStatusRequest;
import com.example.restaurant.dto.request.ReportFilterRequest;
import com.example.restaurant.dto.response.ReportListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;

public interface IReportServices {
    ResultHandler<Void> add(String waiterToken, AddReportRequest request);

    ResultHandler<PagedResult<ReportListResponse>> list(ReportFilterRequest request);

    ResultHandler<Void> changeStatus(String adminToken, ChangeReportStatusRequest request);
}
