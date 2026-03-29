package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.GuestReportStatus;
import com.example.restaurant.repository.interfaces.IReportRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServicesTest {
    @Mock
    private IReportRepository _reportRepo;

    @Mock
    private IUserRepository _userRepo;

    @InjectMocks
    private ReportServices _reportServices;

    @Test
    void add_ShouldCreateReport_WhenUserIsClient() {
        String waiterToken = "WAITER_TOKEN";
        String clientToken = "CLIENT_TOKEN";
        AddReportRequest request = new AddReportRequest();
        request.setClientToken(clientToken);
        request.setReason("Rude behavior");

        Users client = new Users();
        client.setToken(clientToken);

        Users waiter = new Users();
        waiter.setToken(waiterToken);

        GuestReportStatus status = new GuestReportStatus();

        when(_userRepo.findByToken(clientToken)).thenReturn(client);
        when(_userRepo.isInRole("ROLE_CLIENT", clientToken)).thenReturn(true);
        when(_userRepo.findByToken(waiterToken)).thenReturn(waiter);
        when(_reportRepo.findStatusByToken("IN_PROGRESS")).thenReturn(status);

        var result = _reportServices.add(waiterToken, request);

        assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());
        assertEquals("Report created successfully", result.getMessage());

        ArgumentCaptor<GuestReports> reportCaptor = ArgumentCaptor.forClass(GuestReports.class);
        verify(_reportRepo, times(1)).save(reportCaptor.capture());

        GuestReports report = reportCaptor.getValue();
        assertEquals(client, report.getGuest());
        assertEquals(waiter, report.getReporter());
        assertEquals("Rude behavior", report.getReason());
        assertTrue(report.getStatuses().contains(status));
    }

    @Test
    void add_ShouldReturnFailure_WhenUserIsNotClient() {
        String waiterToken = "WAITER_TOKEN";
        String clientToken = "OTHER_STAFF_TOKEN";
        AddReportRequest request = new AddReportRequest();
        request.setClientToken(clientToken);
        request.setReason("Some issue");

        Users notClient = new Users();
        notClient.setToken(clientToken);

        when(_userRepo.findByToken(clientToken)).thenReturn(notClient);
        when(_userRepo.isInRole("ROLE_CLIENT", clientToken)).thenReturn(false);

        var result = _reportServices.add(waiterToken, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertEquals("You can only report users with the client role", result.getMessage());

        verify(_reportRepo, never()).save(any(GuestReports.class));
        verify(_reportRepo, never()).findStatusByToken(anyString());
    }
}
