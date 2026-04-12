package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.ChangeReportStatusRequest;
import com.example.restaurant.dto.response.SyncReportResponse;
import com.example.restaurant.enums.WebSocketEventType;
import com.example.restaurant.mappers.SyncMapper;
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
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.OffsetDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Spy
    private SyncMapper _syncMapper = Mappers.getMapper(SyncMapper.class);

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

        doAnswer(invocation -> {
            GuestReports r = invocation.getArgument(0);
            r.setToken("NEW_REPORT_TOKEN");
            return null;
        }).when(_reportRepo).save(any(GuestReports.class));

        assertDoesNotThrow(() -> _reportServices.add(waiterToken, request));

        verify(_reportRepo, times(1)).save(any(GuestReports.class));
        verify(_notification, times(1)).sendEventToTopic(
                eq("/reports/updates"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.CREATED &&
                                event.getEntityType().equals("REPORT") &&
                                "NEW_REPORT_TOKEN".equals(event.getToken()) &&
                                event.getPayload() != null &&
                                "Inappropriate behavior at the table."
                                        .equals(((SyncReportResponse) event.getPayload()).getReason())
                )
        );
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
    @DisplayName("Change Status: Should create ban and set status to ACCEPTED")
    void changeStatus_AcceptsAndCreatesBan() {
        String adminToken = "ADMIN_TOKEN";
        ChangeReportStatusRequest request = new ChangeReportStatusRequest();
        request.setReportToken("REPORT_TOKEN");
        request.setAccepted(true);
        request.setExpiresAt(OffsetDateTime.now().plusDays(7));

        GuestReports report = new GuestReports();
        report.setGuest(new Users());
        report.setToken("REPORT_TOKEN");
        report.setReason("Toxic behavior");

        when(_reportRepo.findByToken("REPORT_TOKEN")).thenReturn(report);
        when(_userRepo.findByToken(adminToken)).thenReturn(new Users());
        when(_reportRepo.findStatusByToken("ACCEPTED")).thenReturn(new GuestReportStatus());

        assertDoesNotThrow(() -> _reportServices.changeStatus(adminToken, request));

        verify(_banServices, times(1)).add(any());
        verify(_reportRepo, times(1)).save(report);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/reports/updates"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.UPDATED &&
                                event.getEntityType().equals("REPORT") &&
                                "REPORT_TOKEN".equals(event.getToken()) &&
                                event.getPayload() != null
                )
        );
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
        report.setToken("REPORT_TOKEN");

        when(_reportRepo.findByToken("REPORT_TOKEN")).thenReturn(report);
        when(_userRepo.findByToken(adminToken)).thenReturn(new Users());
        when(_reportRepo.findStatusByToken("REJECTED")).thenReturn(new GuestReportStatus());

        assertDoesNotThrow(() -> _reportServices.changeStatus(adminToken, request));

        verify(_banServices, never()).add(any());
        verify(_reportRepo, times(1)).save(report);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/reports/updates"),
                argThat(event ->
                        event != null &&
                                event.getEventType() == WebSocketEventType.UPDATED &&
                                "REPORT".equals(event.getEntityType()) &&
                                "REPORT_TOKEN".equals(event.getToken()) &&
                                event.getPayload() != null
                )
        );
    }
}
