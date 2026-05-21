package com.example.restaurant.services.reservation;

import com.example.restaurant.dto.sync.SyncReservationResponse;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.helpers.WebSocketTopics;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.services.NotificationServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationSyncPublisher {
  private final NotificationServices _notification;
  private final SyncMapper _syncMapper;

  private static final String RESERVATION_ENTITY_TYPE = "RESERVATION";

  public void publishReservationCreate(Reservations reservation) {
    WebSocketEvent<SyncReservationResponse> event =
        WebSocketEvent.created(
            RESERVATION_ENTITY_TYPE,
            reservation.getToken(),
            _syncMapper.toSyncReservationResponse(reservation));
    _notification.sendEventToTopic(WebSocketTopics.RESERVATIONS_TOPIC, event);
  }

  public void publishReservationUpdated(Reservations reservation) {
    WebSocketEvent<SyncReservationResponse> event =
        WebSocketEvent.updated(
            RESERVATION_ENTITY_TYPE,
            reservation.getToken(),
            _syncMapper.toSyncReservationResponse(reservation));
    _notification.sendEventToTopic(WebSocketTopics.RESERVATIONS_TOPIC, event);
  }
}
