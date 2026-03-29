package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.lookup.GuestReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface IReportRepository {
    GuestReportStatus findStatusByToken(String token);

    void save(GuestReports report);

    Page<GuestReports> findAll(Specification<GuestReports> spec, Pageable pageable);
}
