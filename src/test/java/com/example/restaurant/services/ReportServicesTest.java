package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReportFilterRequest;
import com.example.restaurant.dto.response.ReportListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.GuestReportStatus;
import com.example.restaurant.repository.interfaces.IReportRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServicesTest {
    @Mock
    private IReportRepository _reportRepo;

    @Mock
    private IUserRepository _userRepo;

    @InjectMocks
    private ReportServices _reportServices;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

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

    @Test
    void list_ShouldReturnPagedReports_WithTranslatedStatus() {
        ReportFilterRequest request = new ReportFilterRequest();
        request.setPagged(new PaggedRequest());
        request.setSortDirection("DESC");

        Users mockGuest = new Users();
        mockGuest.setUsername("Guest");
        mockGuest.setToken("GUEST_TOKEN");

        Users mockReporter = new Users();
        mockReporter.setUsername("Konfident");
        mockReporter.setToken("KONFIDENT_TOKEN");

        GuestReportStatus mockStatus = new GuestReportStatus();
        mockStatus.setNamePl("W trakcie");
        mockStatus.setNameEn("In progress");

        GuestReports mockReport = new GuestReports();
        mockReport.setToken("report-123");
        mockReport.setReason("Test reason");
        mockReport.setCreatedAt(OffsetDateTime.now());
        mockReport.setGuest(mockGuest);
        mockReport.setReporter(mockReporter);
        mockReport.setStatuses(Set.of(mockStatus));

        Page<GuestReports> mockPage = new PageImpl<>(List.of(mockReport));

        when(_reportRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        ResultHandler<PagedResult<ReportListResponse>> result = _reportServices.list(request);

        assertEquals(HttpStatus.OK.value(), result.getStatusCode());

        PagedResult<ReportListResponse> data = result.getData();
        assertNotNull(data);
        assertEquals(1, data.getItems().size());

        ReportListResponse mappedResponse = data.getItems().get(0);
        assertEquals("report-123", mappedResponse.getToken());
        assertEquals("Guest", mappedResponse.getGuestUsername());
        assertEquals("Konfident", mappedResponse.getReporterUsername());
        assertEquals("Test reason", mappedResponse.getReason());

        assertEquals("In progress", mappedResponse.getStatus());
    }
}
