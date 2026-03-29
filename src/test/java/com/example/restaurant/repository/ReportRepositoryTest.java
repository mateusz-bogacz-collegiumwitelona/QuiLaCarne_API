package com.example.restaurant.repository;

import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.lookup.GuestReportStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaGuestReportRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaGuestReportStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void findStatusByToken_ShouldReturnStatus_WhenExists() {
        GuestReportStatus status = new GuestReportStatus();
        when(_jpaGuestStausRepo.findByToken("TOKEN")).thenReturn(Optional.of(status));

        GuestReportStatus result = _reportRepo.findStatusByToken("TOKEN");

        assertNotNull(result);
        assertEquals(status, result);
        verify(_jpaGuestStausRepo, times(1)).findByToken("TOKEN");
    }

    @Test
    void findStatusByToken_ShouldThrowException_WhenNotFound() {
        when(_jpaGuestStausRepo.findByToken("UNKNOWN")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> _reportRepo.findStatusByToken("UNKNOWN")
        );

        assertEquals("Report Status not found", exception.getMessage());
    }

    @Test
    void save_ShouldCallJpaRepository() {
        GuestReports report = new GuestReports();

        _reportRepo.save(report);

        verify(_jpaReportRepo, times(1)).save(report);
    }
}
