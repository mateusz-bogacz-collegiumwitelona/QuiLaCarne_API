package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.CreateBanDomain;
import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.ChangeReportStatusRequest;
import com.example.restaurant.dto.request.ReportFilterRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.ReportListResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.lookup.GuestReportStatus;
import com.example.restaurant.repository.interfaces.IReportRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IBanServices;
import com.example.restaurant.services.interfaces.IReportServices;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportServices implements IReportServices {
    private final IReportRepository _reportRepo;
    private final IUserRepository _userRepo;
    private final IBanServices _banServices;
    private final NotificationServices _notification;


    @Override
    @Transactional
    @Auditable(action = "ADD_REPORT")
    public void add(String waiterToken, AddReportRequest request) {
        var client = _userRepo.findByToken(request.getClientToken());

        if (!_userRepo.isInRole("ROLE_CLIENT", client.getToken()))
            throw new IllegalStateException("You can only report users with the client role");

        var waiter = _userRepo.findByToken(waiterToken);
        var status = _reportRepo.findStatusByToken("IN_PROGRESS");

        GuestReports report = new GuestReports();
        report.setGuest(client);
        report.setReporter(waiter);
        report.setReason(request.getReason().trim());
        report.setStatuses(Set.of(status));

        _notification.sendToTopic("reports", "New report from waiter: " + waiterToken);

        _reportRepo.save(report);
    }

    @Override
    public PagedResult<ReportListResponse> list(ReportFilterRequest request) {
        if (request.getFromDate() != null && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate()))
            throw new IllegalStateException("Start date cannot be after end date");


        String lang = LocaleContextHolder.getLocale().getLanguage();

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                request.getPagged().getPage() - 1,
                request.getPagged().getSize(),
                Sort.by(direction, "createdAt")
        );

        Specification<GuestReports> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getFromDate() != null)
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), request.getFromDate()));

            if (request.getToDate() != null)
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), request.getToDate()));

            if (request.getStatusToken() != null && !request.getStatusToken().isEmpty()) {
                Join<GuestReports, GuestReportStatus> statusJoin = root.join("statuses");
                predicates.add(criteriaBuilder.equal(statusJoin.get("token"), request.getStatusToken()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<GuestReports> page = _reportRepo.findAll(spec, pageable);

        return new PagedResult<>(page.map(r -> {
            String translatedStatus = "UNKNOWN";

            if (r.getStatuses() != null && !r.getStatuses().isEmpty()) {
                var status = r.getStatuses().iterator().next();

                translatedStatus = "pl".equalsIgnoreCase(lang)
                        ? status.getNamePl()
                        : status.getNameEn();
            }

            return ReportListResponse.builder()
                    .token(r.getToken())
                    .guestUsername(r.getGuest().getUsername())
                    .guestToken(r.getGuest().getToken())
                    .reporterUsername(r.getReporter().getUsername())
                    .reporterToken(r.getReporter().getToken())
                    .reason(r.getReason())
                    .createdAt(r.getCreatedAt())
                    .status(translatedStatus)
                    .build();
        }));
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_REPORT_STATUS")
    public void changeStatus(String adminToken, ChangeReportStatusRequest request) {
        var report = _reportRepo.findByToken(request.getReportToken());

        var admin = _userRepo.findByToken(adminToken);

        if (request.isAccepted()) {
            if (request.getExpiresAt() == null ||
                    request.getExpiresAt().isBefore(OffsetDateTime.now()))
                throw new IllegalStateException("A valid future expiration date is required to accept a report and issue a ban");


            CreateBanDomain banDomain = new CreateBanDomain(
                    report.getGuest(),
                    admin,
                    report.getReason(),
                    request.getExpiresAt()
            );

            _banServices.add(banDomain);

            var status = _reportRepo.findStatusByToken("ACCEPTED");
            report.setStatuses(Set.of(status));

        } else {
            var status = _reportRepo.findStatusByToken("REJECTED");
            report.setStatuses(Set.of(status));
        }

        _reportRepo.save(report);

        _notification.sendToTopic("reports/updates", "Report resolved: " + request.getReportToken());
    }

    @Override
    @Cacheable(
            value = "reportStatuses",
            key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public DictionaryResponse getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return new DictionaryResponse(DictionaryHelper.map(_reportRepo.findAllStatuses(), lang));
    }
}
