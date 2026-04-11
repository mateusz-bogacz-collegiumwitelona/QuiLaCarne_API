package com.example.restaurant.repository;

import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.lookup.GuestReportStatus;
import com.example.restaurant.repository.interfaces.IReportRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaGuestReportRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaGuestReportStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportRepository implements IReportRepository {
    private final IJpaGuestReportRepository _jpaRepostRepo;
    private final IJpaGuestReportStatusRepository _jpaReportStatusRepo;

    @Override
    public GuestReportStatus findStatusByToken(String token) {
        return _jpaReportStatusRepo.findByToken(token)
                .orElseThrow(
                        () -> new EntityNotFoundException("Report Status not found")
                );
    }

    @Override
    public void save(GuestReports report) {
        _jpaRepostRepo.save(report);
    }

    @Override
    public Page<GuestReports> findAll(Specification<GuestReports> spec, Pageable pageable) {
        return _jpaRepostRepo.findAll(spec, pageable);
    }

    @Override
    public GuestReports findByToken(String token) {
        return _jpaRepostRepo.findByToken(token);
    }

    @Override
    public List<GuestReportStatus> findAllStatuses() {
        return _jpaReportStatusRepo.findAll();
    }

    @Override
    public long countStatuses() {
        return _jpaReportStatusRepo.count();
    }

    @Override
    public long count() {
        return _jpaRepostRepo.count();
    }
}
