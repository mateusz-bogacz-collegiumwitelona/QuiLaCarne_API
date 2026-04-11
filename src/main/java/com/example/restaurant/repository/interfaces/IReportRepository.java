package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.lookup.GuestReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface IReportRepository {
    GuestReportStatus findStatusByToken(String token);

    void save(GuestReports report);

    Page<GuestReports> findAll(Specification<GuestReports> spec, Pageable pageable);

    GuestReports findByToken(String token);

    List<GuestReportStatus> findAllStatuses();

    long countStatuses();

    long count();
}
