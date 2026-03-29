package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.GuestReports;
import com.example.restaurant.repository.interfaces.IReportRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IReportServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportServices implements IReportServices {
    private final IReportRepository _reportRepo;
    private final IUserRepository _userRepo;

    @Override
    @Transactional
    public ResultHandler<Void> add(String waiterToken, AddReportRequest request) {
        var client = _userRepo.findByToken(request.getClientToken());

        if (!_userRepo.isInRole("ROLE_CLIENT", client.getToken()))
            return ResultHandler.failure(
                    "You can only report users with the client role",
                    HttpStatus.BAD_REQUEST.value()
            );

        var waiter = _userRepo.findByToken(waiterToken);
        var status = _reportRepo.findStatusByToken("IN_PROGRESS");

        GuestReports report = new GuestReports();
        report.setGuest(client);
        report.setReporter(waiter);
        report.setReason(request.getReason());
        report.setStatuses(Set.of(status));

        _reportRepo.save(report);

        return ResultHandler.success(
                "Report created successfully",
                HttpStatus.CREATED.value()
        );
    }
}
