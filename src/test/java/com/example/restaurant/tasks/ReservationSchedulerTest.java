package com.example.restaurant.tasks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.services.EmailServices;
import com.example.restaurant.services.reservation.ReservationSyncPublisher;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationSchedulerTest {

  @Mock private IReservationRepository _reservationRepo;
  @Mock private EmailServices _emailServices;
  @Mock private ReservationSyncPublisher _reservationSyncPublisher;

  @InjectMocks private ReservationScheduler _reservationScheduler;

  @Test
  @DisplayName("changeStatus: Should do nothing when there are no expired reservations")
  void changeStatus_ShouldDoNothing_WhenNoExpiredReservations() {
    when(_reservationRepo.findExpiredActiveReservations(any(OffsetDateTime.class)))
        .thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> _reservationScheduler.handleReservationNoShow());

    verify(_reservationRepo, never()).findStatusByToken(anyString());
    verify(_reservationRepo, never()).saveAll(any());
    verifyNoInteractions(_emailServices);
    verifyNoInteractions(_reservationSyncPublisher);
  }

  @Test
  @DisplayName(
      "changeStatus: Should update status, save and send notifications for expired reservations")
  void changeStatus_ShouldCancelAndNotify_WhenExpiredReservationsExist() {
    ReservationStatus cancelledStatus = new ReservationStatus();
    cancelledStatus.setToken("NO_SHOW");

    Users mockUser = new Users();
    mockUser.setEmail("test@example.com");
    mockUser.setUsername("testUser");

    Reservations resWithUser = new Reservations();
    resWithUser.setToken("RES_1");
    resWithUser.setUser(mockUser);

    Reservations resWithoutUser = new Reservations();
    resWithoutUser.setToken("RES_2");
    resWithoutUser.setUser(null);

    List<Reservations> expiredReservations = List.of(resWithUser, resWithoutUser);

    when(_reservationRepo.findExpiredActiveReservations(any(OffsetDateTime.class)))
        .thenReturn(expiredReservations);
    when(_reservationRepo.findStatusByToken("NO_SHOW")).thenReturn(cancelledStatus);

    assertDoesNotThrow(() -> _reservationScheduler.handleReservationNoShow());

    verify(_reservationRepo, times(1)).saveAll(expiredReservations);

    verify(_emailServices, times(1))
        .sendEmailReservationCancelled(eq("test@example.com"), eq("testUser"));

    verify(_reservationSyncPublisher, times(1)).publishReservationUpdated(resWithUser);
    verify(_reservationSyncPublisher, times(1)).publishReservationUpdated(resWithoutUser);
  }

  @Test
  @DisplayName("changeStatus: Should catch exception and not crash when database fails")
  void changeStatus_ShouldCatchException_WhenDatabaseFails() {
    when(_reservationRepo.findExpiredActiveReservations(any(OffsetDateTime.class)))
        .thenThrow(new RuntimeException("Database connection error"));

    assertDoesNotThrow(() -> _reservationScheduler.handleReservationNoShow());

    verify(_reservationRepo, never()).saveAll(any());
    verifyNoInteractions(_emailServices);
    verifyNoInteractions(_reservationSyncPublisher);
  }
}
