package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.CreateBanDomain;
import com.example.restaurant.dto.payload.ReportPayload;
import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.ChangeReportStatusRequest;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.models.GuestReports;
import com.example.restaurant.repository.interfaces.IReportRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IBanServices;
import com.example.restaurant.services.interfaces.IReportServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportServices implements IReportServices {
    private final IReportRepository _reportRepo;
    private final IUserRepository _userRepo;
    private final IBanServices _banServices;
    private final NotificationServices _notification;

    private static final String REPORT_ENTITY_TYPE = "REPORT";

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

        _reportRepo.save(report);

        WebSocketEvent<ReportPayload> event = WebSocketEvent.created(
                REPORT_ENTITY_TYPE,
                report.getToken(),
                createPayload(report)
        );
        _notification.sendEventToTopic("/reports/updates", event);
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

        WebSocketEvent<ReportPayload> event = WebSocketEvent.updated(
                REPORT_ENTITY_TYPE,
                report.getToken(),
                createPayload(report)
        );
        _notification.sendEventToTopic("/reports/updates", event);
    }

    private ReportPayload createPayload(GuestReports report) {
        String statusToken = null;
        if (report.getStatuses() != null && !report.getStatuses().isEmpty()) {
            statusToken = report.getStatuses().iterator().next().getToken();
        }

        return ReportPayload.builder()
                .token(report.getToken())
                .guestToken(report.getGuest() != null ? report.getGuest().getToken() : null)
                .reporterToken(report.getReporter() != null ? report.getReporter().getToken() : null)
                .reason(report.getReason())
                .statusToken(statusToken)
                .createdAt(report.getCreatedAt())
                .build();
    }
}
