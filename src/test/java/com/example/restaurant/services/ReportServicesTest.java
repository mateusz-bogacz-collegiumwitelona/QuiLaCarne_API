package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.ChangeReportStatusRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReportFilterRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.dto.response.ReportListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.GuestReportStatus;
import com.example.restaurant.repository.interfaces.IReportRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IBanServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

    @Mock
    private IBanServices _banServices;

    @Mock
    private NotificationServices _notification;

    @InjectMocks
    private ReportServices _reportServices;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Add Report: Success - Should save report when target is a client")
    void add_Successful() {
        String waiterToken = "WAITER_123";
        AddReportRequest request = new AddReportRequest();
        request.setClientToken("CLIENT_456");
        request.setReason("Inappropriate behavior at the table.");

        Users client = new Users();
        client.setToken("CLIENT_456");

        when(_userRepo.findByToken("CLIENT_456")).thenReturn(client);
        when(_userRepo.isInRole("ROLE_CLIENT", "CLIENT_456")).thenReturn(true);
        when(_userRepo.findByToken(waiterToken)).thenReturn(new Users());
        when(_reportRepo.findStatusByToken("IN_PROGRESS")).thenReturn(new GuestReportStatus());

        assertDoesNotThrow(() -> _reportServices.add(waiterToken, request));

        verify(_reportRepo, times(1)).save(any(GuestReports.class));
        verify(_notification, times(1)).sendToTopic(eq("reports"), anyString());
    }

    @Test
    @DisplayName("Add Report: Should throw IllegalStateException when target is not a client")
    void add_ThrowsException_WhenTargetIsNotClient() {
        AddReportRequest request = new AddReportRequest();
        request.setClientToken("STAFF_789");

        Users client = new Users();
        client.setToken("STAFF_789");

        when(_userRepo.findByToken("STAFF_789")).thenReturn(client);
        when(_userRepo.isInRole("ROLE_CLIENT", "STAFF_789")).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> _reportServices.add("WAITER_123", request));
        verify(_reportRepo, never()).save(any());
    }

    @Test
    @DisplayName("List Reports: Should return PagedResult with mapped data")
    void list_ReturnsPagedResult() {
        ReportFilterRequest filter = new ReportFilterRequest();
        filter.setPagged(new PaggedRequest());

        GuestReportStatus status = new GuestReportStatus();
        status.setNameEn("Pending Review");

        GuestReports report = new GuestReports();
        report.setGuest(new Users());
        report.setReporter(new Users());
        report.setStatuses(Set.of(status));
        report.setReason("Test reason");

        when(_reportRepo.findAll(ArgumentMatchers.any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        PagedResult<ReportListResponse> result = _reportServices.list(filter);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals("Pending Review", result.getItems().getFirst().getStatus());
        assertEquals("Test reason", result.getItems().getFirst().getReason());
    }

    @Test
    @DisplayName("Change Status: Should create ban and set status to ACCEPTED")
    void changeStatus_AcceptsAndCreatesBan() {
        String adminToken = "ADMIN_TOKEN";
        ChangeReportStatusRequest request = new ChangeReportStatusRequest();
        request.setReportToken("REPORT_TOKEN");
        request.setAccepted(true);
        request.setExpiresAt(OffsetDateTime.now().plusDays(7));

        GuestReports report = new GuestReports();
        report.setGuest(new Users());
        report.setReason("Toxic behavior");

        when(_reportRepo.findByToken("REPORT_TOKEN")).thenReturn(report);
        when(_userRepo.findByToken(adminToken)).thenReturn(new Users());
        when(_reportRepo.findStatusByToken("ACCEPTED")).thenReturn(new GuestReportStatus());

        assertDoesNotThrow(() -> _reportServices.changeStatus(adminToken, request));

        verify(_banServices, times(1)).create(any());
        verify(_reportRepo, times(1)).save(report);
        verify(_notification, times(1)).sendToTopic(eq("reports/updates"), anyString());
    }

    @Test
    @DisplayName("Change Status: Should throw IllegalStateException when accepted but date is invalid")
    void changeStatus_ThrowsException_WhenDateIsInvalid() {
        ChangeReportStatusRequest request = new ChangeReportStatusRequest();
        request.setAccepted(true);
        request.setExpiresAt(OffsetDateTime.now().minusDays(1));

        when(_reportRepo.findByToken(any())).thenReturn(new GuestReports());

        assertThrows(IllegalStateException.class, () -> _reportServices.changeStatus("ADMIN_TOKEN", request));
    }

    @Test
    @DisplayName("Change Status: Should set status to REJECTED and not create ban")
    void changeStatus_RejectsReport() {
        String adminToken = "ADMIN_TOKEN";
        ChangeReportStatusRequest request = new ChangeReportStatusRequest();
        request.setReportToken("REPORT_TOKEN");
        request.setAccepted(false);

        GuestReports report = new GuestReports();
        when(_reportRepo.findByToken("REPORT_TOKEN")).thenReturn(report);
        when(_userRepo.findByToken(adminToken)).thenReturn(new Users());
        when(_reportRepo.findStatusByToken("REJECTED")).thenReturn(new GuestReportStatus());

        assertDoesNotThrow(() -> _reportServices.changeStatus(adminToken, request));

        verify(_banServices, never()).create(any());
        verify(_reportRepo, times(1)).save(report);
        verify(_notification, times(1)).sendToTopic(eq("reports/updates"), anyString());
    }

    @Test
    @DisplayName("getDictionary: Returns empty list when repository returns empty")
    void getDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
        when(_reportRepo.findAllStatuses()).thenReturn(new java.util.ArrayList<>());

        List<EntityResponse> result = _reportServices.getDictionary();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getDictionary: Returns mapped elements with Polish names when language is pl")
    void getDictionary_ShouldReturnPolishNames_WhenLanguageIsPl() {
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_PL));

        GuestReportStatus status = new GuestReportStatus();
        status.setToken(TestConstants.STATUS_IN_PROGRESS);
        status.setNamePl("W trakcie PL");
        status.setNameEn("In Progress EN");

        when(_reportRepo.findAllStatuses()).thenReturn(List.of(status));

        List<EntityResponse> result = _reportServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals(TestConstants.STATUS_IN_PROGRESS, result.getFirst().getToken());
        assertEquals("W trakcie PL", result.getFirst().getName());
    }

    @Test
    @DisplayName("getDictionary: Returns mapped elements with English names when language is not pl")
    void getDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_EN));
        GuestReportStatus status = new GuestReportStatus();
        status.setToken(TestConstants.STATUS_ACCEPTED);
        status.setNamePl("Zaakceptowane PL");
        status.setNameEn("Accepted EN");

        when(_reportRepo.findAllStatuses()).thenReturn(List.of(status));

        List<EntityResponse> result = _reportServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals(TestConstants.STATUS_ACCEPTED, result.getFirst().getToken());
        assertEquals("Accepted EN", result.getFirst().getName());
    }
}
