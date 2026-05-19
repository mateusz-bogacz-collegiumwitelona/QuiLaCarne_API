package com.example.restaurant.tasks;

import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.services.EmailServices;
import com.example.restaurant.services.reservation.ReservationSyncPublisher;
import java.time.OffsetDateTime;
import java.util.Set;
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

  @Scheduled(fixedDelay = 15 * 60000)
  public void handleReservationNoShow() {
    try {
      OffsetDateTime deadline = OffsetDateTime.now().minusMinutes(30);
      var expiredReservations = _reservationRepo.findExpiredActiveReservations(deadline);

      if (expiredReservations.isEmpty()) return;

      ReservationStatus cancelledStatus = _reservationRepo.findStatusByToken("NO_SHOW");

      for (var reservation : expiredReservations) {
        reservation.setReservationStatus(Set.of(cancelledStatus));
      }

      _reservationRepo.saveAll(expiredReservations);
      log.info(
          "Cancellation of {} reservations has been saved in the database.",
          expiredReservations.size());

      for (var reservation : expiredReservations) {
        if (reservation.getUser() != null) {
          _emailServices.sendEmailReservationCancelled(
              reservation.getUser().getEmail(), reservation.getUser().getUsername());
        }
        _reservationSyncPublisher.publishReservationUpdated(reservation);
      }
    } catch (Exception e) {
      log.error("An error occurred while automatically canceling your reservation.", e);
    }
  }
}
