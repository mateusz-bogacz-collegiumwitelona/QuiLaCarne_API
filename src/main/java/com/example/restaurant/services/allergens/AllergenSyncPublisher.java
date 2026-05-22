package com.example.restaurant.services.allergens;

import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.helpers.staics.WebSocketEntityType;
import com.example.restaurant.helpers.staics.WebSocketTopics;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.services.NotificationServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllergenSyncPublisher {
  private final NotificationServices _notification;
  private final SyncMapper _syncMapper;

  public void publishAllergenCreate(Allergens allergen) {
    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(allergen);
    WebSocketEvent<SyncDictionaryResponse> event =
        WebSocketEvent.created(
            WebSocketEntityType.ALLERGENS_ENTITY_TYPE, allergen.getToken(), payload);
    _notification.sendEventToTopic(WebSocketTopics.DICTIONARY_ALLERGENS, event);
  }

  public void publishAllergenDelete(String token) {
    WebSocketEvent<Void> event =
        WebSocketEvent.deleted(WebSocketEntityType.ALLERGENS_ENTITY_TYPE, token);

    _notification.sendEventToTopic(WebSocketTopics.DICTIONARY_ALLERGENS, event);
  }
}
