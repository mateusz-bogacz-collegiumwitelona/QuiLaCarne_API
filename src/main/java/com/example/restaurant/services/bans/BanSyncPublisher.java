package com.example.restaurant.services.bans;

import com.example.restaurant.dto.sync.SyncBanResponse;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.helpers.staics.WebSocketEntityType;
import com.example.restaurant.helpers.staics.WebSocketTopics;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Bans;
import com.example.restaurant.services.NotificationServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BanSyncPublisher {
  private final NotificationServices _notification;
  private final SyncMapper _syncMapper;

  public void publishBanCreated(Bans ban) {
    WebSocketEvent<SyncBanResponse> event =
        WebSocketEvent.created(
            WebSocketEntityType.BANS_ENTITY_TYPE,
            ban.getToken(),
            _syncMapper.toBanSyncResponse(ban));
    _notification.sendEventToTopic(WebSocketTopics.BAN_TOPIC, event);
  }

  public void publishBanUpdate(Bans ban) {
    WebSocketEvent<SyncBanResponse> event =
        WebSocketEvent.updated(
            WebSocketEntityType.BANS_ENTITY_TYPE,
            ban.getToken(),
            _syncMapper.toBanSyncResponse(ban));
    _notification.sendEventToTopic(WebSocketTopics.BAN_TOPIC, event);
  }
}
