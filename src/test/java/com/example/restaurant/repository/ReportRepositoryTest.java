package com.example.restaurant.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.restaurant.TestConstants;
import com.example.restaurant.models.GuestReports;
import com.example.restaurant.models.lookup.GuestReportStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaGuestReportRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaGuestReportStatusRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class ReportRepositoryTest {
  @Mock private IJpaGuestReportRepository _jpaReportRepo;

  @Mock private IJpaGuestReportStatusRepository _jpaStausRepo;

  @InjectMocks private ReportRepository _reportRepo;

  @Test
  @DisplayName("Find status by token: should return status id exists")
  void findStatusByToken_ShouldReturnStatus_WhenExists() {
    GuestReportStatus status = new GuestReportStatus();
    when(_jpaStausRepo.findByToken(TestConstants.TOKEN_NON_EXISTENT))
        .thenReturn(Optional.of(status));

    GuestReportStatus result = _reportRepo.findStatusByToken(TestConstants.TOKEN_NON_EXISTENT);

    assertNotNull(result);
    assertEquals(status, result);
    verify(_jpaStausRepo, times(1)).findByToken(TestConstants.TOKEN_NON_EXISTENT);
  }

  @Test
  @DisplayName("Find status by token: should throw exception if doesn't exists")
  void findStatusByToken_ShouldThrowException_WhenNotFound() {
    when(_jpaStausRepo.findByToken("UNKNOWN")).thenReturn(Optional.empty());

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> _reportRepo.findStatusByToken("UNKNOWN"));

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
    GuestReports mockReport = new GuestReports();
    mockReport.setToken(TestConstants.FAKE_REPORT_TOKEN);

    when(_jpaReportRepo.findByToken(TestConstants.FAKE_REPORT_TOKEN)).thenReturn(mockReport);

    GuestReports result = _reportRepo.findByToken(TestConstants.FAKE_REPORT_TOKEN);

    assertNotNull(result);
    assertEquals(TestConstants.FAKE_REPORT_TOKEN, result.getToken());

    verify(_jpaReportRepo, times(1)).findByToken(TestConstants.FAKE_REPORT_TOKEN);
  }

  @Test
  @DisplayName("findAllStatuses: Should return list of statuses from JPA")
  void findAllStatuses_ShouldReturnListOfStatuses() {
    List<GuestReportStatus> expectedStatuses =
        List.of(new GuestReportStatus(), new GuestReportStatus());

    when(_jpaStausRepo.findAll()).thenReturn(expectedStatuses);

    List<GuestReportStatus> result = _reportRepo.findAllStatuses();

    assertEquals(expectedStatuses, result);
    verify(_jpaStausRepo, times(1)).findAll();
  }
}
