package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.ChangeReportStatusRequest;

public interface IReportServices {
    void add(String waiterToken, AddReportRequest request);

    void changeStatus(String adminToken, ChangeReportStatusRequest request);
}
