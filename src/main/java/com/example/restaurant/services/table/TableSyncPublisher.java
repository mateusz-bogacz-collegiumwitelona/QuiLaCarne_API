package com.example.restaurant.services.table;

import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.dto.sync.SyncTableResponse;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.helpers.staics.WebSocketEntityType;
import com.example.restaurant.helpers.staics.WebSocketTopics;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.services.NotificationServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TableSyncPublisher {
  private final NotificationServices _notification;
  private final SyncMapper _syncMapper;

  public void publishTableCreate(RestaurantTables table) {
    WebSocketEvent<SyncTableResponse> event =
        WebSocketEvent.created(
            WebSocketEntityType.TABLE_ENTITY_TYPE,
            table.getToken(),
            _syncMapper.toSyncTableResponse(table));
    _notification.sendEventToTopic(WebSocketTopics.TABLE_TOPIC, event);
  }

  public void publishTableDelete(String token) {
    WebSocketEvent<Void> event =
        WebSocketEvent.deleted(WebSocketEntityType.TABLE_ENTITY_TYPE, token);
    _notification.sendEventToTopic(WebSocketTopics.TABLE_TOPIC, event);
  }

  public void publishTableStatusCreate(TableStatus status) {
    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(status);
    WebSocketEvent<SyncDictionaryResponse> event =
        WebSocketEvent.created(
            WebSocketEntityType.TABLE_STATUS_ENTITY_TYPE, status.getToken(), payload);
    _notification.sendEventToTopic(WebSocketTopics.TABLE_STATUS_TOPIC, event);
  }

  public void publishTableStatusDelete(String token) {
    WebSocketEvent<Void> event =
        WebSocketEvent.deleted(WebSocketEntityType.TABLE_STATUS_ENTITY_TYPE, token);
    _notification.sendEventToTopic(WebSocketTopics.TABLE_STATUS_TOPIC, event);
  }

  public void publishTableUpdate(RestaurantTables table) {
    WebSocketEvent<SyncTableResponse> event =
        WebSocketEvent.updated(
            WebSocketEntityType.TABLE_ENTITY_TYPE,
            table.getToken(),
            _syncMapper.toSyncTableResponse(table));
    _notification.sendEventToTopic(WebSocketTopics.TABLE_TOPIC, event);
  }
}
