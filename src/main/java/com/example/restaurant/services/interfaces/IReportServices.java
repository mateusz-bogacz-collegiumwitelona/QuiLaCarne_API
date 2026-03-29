package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.helpers.ResultHandler;

public interface IReportServices {
    ResultHandler<Void> add(String waiterToken, AddReportRequest request);
}
