package com.example.restaurant.state;

import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import java.util.HashSet;
import java.util.Set;

public enum ReservationStateLogic {
  ACTIVE {
    @Override
    public void assignWaiter(Reservations reservation, ReservationStatus newStatus) {
      reservation.setReservationStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void markAsAbsent(Reservations reservation, ReservationStatus newStatus) {
      reservation.setReservationStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void cancel(Reservations reservation, ReservationStatus newStatus) {
      reservation.setReservationStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void markAsCompleted(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Operation not allowed in current status.");
    }
  },

  IN_PROGRESS {
    @Override
    public void assignWaiter(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException(
          "A waiter has already been assigned (reservation is in progress).");
    }

    @Override
    public void markAsAbsent(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException(
          "You cannot mark a reservation that is in progress as NO_SHOW.");
    }

    @Override
    public void cancel(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException(
          "You cannot cancel a reservation that is already in progress.");
    }

    @Override
    public void markAsCompleted(Reservations reservation, ReservationStatus newStatus) {
      reservation.setReservationStatus(new HashSet<>(Set.of(newStatus)));
    }
  },

  NO_SHOW {
    @Override
    public void assignWaiter(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException(
          "The guest did not show up, the waiter could not be assigned.");
    }

    @Override
    public void markAsAbsent(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("The reservation is already marked as NO_SHOW.");
    }

    @Override
    public void cancel(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("NO_SHOW reservations cannot be canceled.");
    }

    @Override
    public void markAsCompleted(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Operation not allowed in current status.");
    }
  },

  CANCELLED {
    @Override
    public void assignWaiter(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("The reservation is canceled, a waiter cannot be assigned.");
    }

    @Override
    public void markAsAbsent(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Reservation is canceled, cannot be marked as NO_SHOW.");
    }

    @Override
    public void cancel(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("The reservation is already canceled.");
    }

    @Override
    public void markAsCompleted(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Operation not allowed in current status.");
    }
  },

  OTHER {
    @Override
    public void assignWaiter(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Operation not allowed in current status.");
    }

    @Override
    public void markAsAbsent(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Operation not allowed in current status.");
    }

    @Override
    public void cancel(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Operation not allowed in current status.");
    }

    @Override
    public void markAsCompleted(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Operation not allowed in current status.");
    }
  },
  COMPLETED {
    public void assignWaiter(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("The reservation is complete, a waiter cannot be assigned.");
    }

    @Override
    public void markAsAbsent(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Reservation is complete, cannot be marked as NO_SHOW.");
    }

    @Override
    public void cancel(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Reservation is complete, cannot be marked as CANCELLED.");
    }

    @Override
    public void markAsCompleted(Reservations reservation, ReservationStatus newStatus) {
      throw new IllegalStateException("Operation not allowed in current status.");
    }
  };

  public abstract void assignWaiter(Reservations reservation, ReservationStatus newStatus);

  public abstract void markAsAbsent(Reservations reservation, ReservationStatus newStatus);

  public abstract void cancel(Reservations reservation, ReservationStatus newStatus);

  public abstract void markAsCompleted(Reservations reservation, ReservationStatus newStatus);

  public static ReservationStateLogic from(Reservations reservation) {
    if (reservation.getReservationStatus() == null
        || reservation.getReservationStatus().isEmpty()) {
      return OTHER;
    }
    String token = reservation.getReservationStatus().iterator().next().getToken();
    try {
      return ReservationStateLogic.valueOf(token);
    } catch (IllegalArgumentException e) {
      return OTHER;
    }
  }
}
