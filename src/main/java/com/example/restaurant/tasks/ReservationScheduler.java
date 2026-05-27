package com.example.restaurant.tasks;

import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.services.EmailServices;
import com.example.restaurant.services.interfaces.ITableServices;
import com.example.restaurant.services.reservation.ReservationSyncPublisher;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {
  private final IReservationRepository _reservationRepo;
  private final EmailServices _emailServices;
  private final ReservationSyncPublisher _reservationSyncPublisher;
  private final ITableServices _tableServices;

  @Scheduled(fixedDelay = 15 * 60000)
  public void handleReservationNoShow() {
    try {
      OffsetDateTime deadline = OffsetDateTime.now().minusMinutes(30);
      var expiredReservations = _reservationRepo.findExpiredActiveReservations(deadline);

      processReservations(
          expiredReservations,
          "NO_SHOW",
          "Cancellation of",
          reservation -> {
            if (reservation.getUser() != null) {
              _emailServices.sendEmailReservationCancelled(
                  reservation.getUser().getEmail(), reservation.getUser().getUsername());
            }
          });
    } catch (Exception e) {
      log.error("An error occurred while automatically canceling reservations.", e);
    }
  }

  @Scheduled(fixedDelay = 15 * 60000)
  public void handleReservationInProgress() {
    try {
      OffsetDateTime deadline = OffsetDateTime.now().minusMinutes(30);
      var inProgressReservations = _reservationRepo.findExpiredInProgressReservations(deadline);

      processReservations(
          inProgressReservations,
          "COMPLETED",
          "Completion of",
          reservation -> {
            if (reservation.getTableId() != null) {
              _tableServices.changeStatusToClean(reservation.getTableId().getToken());
            }
          });
    } catch (Exception e) {
      log.error("An error occurred while automatically completing in-progress reservations.", e);
    }
  }

  private void processReservations(
      List<Reservations> reservations,
      String targetStatusToken,
      String logPrefix,
      Consumer<Reservations> additionalPostSaveAction) {

    if (reservations.isEmpty()) return;

    ReservationStatus targetStatus = _reservationRepo.findStatusByToken(targetStatusToken);

    for (var reservation : reservations) {
      reservation.setReservationStatus(Set.of(targetStatus));
    }

    _reservationRepo.saveAll(reservations);
    log.info("{} {} reservations has been saved in the database.", logPrefix, reservations.size());

    for (var reservation : reservations) {
      if (additionalPostSaveAction != null) additionalPostSaveAction.accept(reservation);

      _reservationSyncPublisher.publishReservationUpdated(reservation);
    }
  }
}
