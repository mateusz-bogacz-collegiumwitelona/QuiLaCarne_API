package com.example.restaurant.repository;

import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.lookup.GuestReportStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaGuestReportRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaGuestReportStatusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportRepositoryTest {
    @Mock
    private IJpaGuestReportRepository _jpaReportRepo;

    @Mock
    private IJpaGuestReportStatusRepository _jpaGuestStausRepo;

    @InjectMocks
    private ReportRepository _reportRepo;

    @Test
    @DisplayName("Find status by token: should return status id exists")
    void findStatusByToken_ShouldReturnStatus_WhenExists() {
        GuestReportStatus status = new GuestReportStatus();
        when(_jpaGuestStausRepo.findByToken("TOKEN")).thenReturn(Optional.of(status));

        GuestReportStatus result = _reportRepo.findStatusByToken("TOKEN");

        assertNotNull(result);
        assertEquals(status, result);
        verify(_jpaGuestStausRepo, times(1)).findByToken("TOKEN");
    }

    @Test
    @DisplayName("Find status by token: should throw exception if doesn't exists")
    void findStatusByToken_ShouldThrowException_WhenNotFound() {
        when(_jpaGuestStausRepo.findByToken("UNKNOWN")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> _reportRepo.findStatusByToken("UNKNOWN")
        );

        assertEquals("Report Status not found", exception.getMessage());
    }

    @Test
    @DisplayName("Save: should call JPA")
    void save_ShouldCallJpaRepository() {
        GuestReports report = new GuestReports();

        _reportRepo.save(report);

        verify(_jpaReportRepo, times(1)).save(report);
    }

    @Test
    @DisplayName("Find all: should return page")
    void findAll_ShouldReturnPageFromJpaRepository() {
        Specification<GuestReports> mockSpec = mock(Specification.class);
        Pageable mockPageable = mock(Pageable.class);
        Page<GuestReports> expectedPage = new PageImpl<>(Collections.emptyList());

        when(_jpaReportRepo.findAll(mockSpec, mockPageable)).thenReturn(expectedPage);

        Page<GuestReports> result = _reportRepo.findAll(mockSpec, mockPageable);

        assertNotNull(result);
        assertEquals(expectedPage, result);

        verify(_jpaReportRepo, times(1)).findAll(mockSpec, mockPageable);
    }

    @Test
    @DisplayName("Find by toke: should return report")
    void findByToken_ShouldReturnReport_WhenCalled() {
        String token = "report-token-123";
        GuestReports mockReport = new GuestReports();
        mockReport.setToken(token);

        when(_jpaReportRepo.findByToken(token)).thenReturn(mockReport);

        GuestReports result = _reportRepo.findByToken(token);

        assertNotNull(result);
        assertEquals(token, result.getToken());

        verify(_jpaReportRepo, times(1)).findByToken(token);
    }
}