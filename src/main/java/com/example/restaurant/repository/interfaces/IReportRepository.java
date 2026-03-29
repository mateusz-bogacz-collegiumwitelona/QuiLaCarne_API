package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.lookup.GuestReportStatus;

public interface IReportRepository {
    GuestReportStatus findStatusByToken(String token);

    void save(GuestReports report);
}
