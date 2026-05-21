package com.example.restaurant.services.user;

import com.example.restaurant.dto.sync.SyncUserResponse;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.helpers.WebSocketTopics;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Users;
import com.example.restaurant.services.NotificationServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncPublisher {
  private final NotificationServices _notification;
  private final SyncMapper _syncMapper;

  private static final String EMPLOYEE_ENTITY_TYPE = "EMPLOYEE";

  public void publishUserCreated(Users user) {
    log.debug("Publishing user created event for: {}", user.getToken());
    sendUserEvent(user, true);
  }

  public void publishUserUpdated(Users user) {
    log.debug("Publishing user updated event for: {}", user.getToken());
    sendUserEvent(user, false);
  }

  public void publishUserDeleted(String userToken) {
    log.debug("Publishing user deleted event for: {}", userToken);
    WebSocketEvent<Void> event = WebSocketEvent.deleted(EMPLOYEE_ENTITY_TYPE, userToken);
    _notification.sendEventToTopic(WebSocketTopics.PERSONNEL_TOPIC, event);
  }

  private void sendUserEvent(Users user, boolean isNew) {
    SyncUserResponse payload = _syncMapper.toSyncUserResponse(user);

    WebSocketEvent<SyncUserResponse> event =
        isNew
            ? WebSocketEvent.created(EMPLOYEE_ENTITY_TYPE, user.getToken(), payload)
            : WebSocketEvent.updated(EMPLOYEE_ENTITY_TYPE, user.getToken(), payload);

    _notification.sendEventToTopic(WebSocketTopics.PERSONNEL_TOPIC, event);
  }
}
