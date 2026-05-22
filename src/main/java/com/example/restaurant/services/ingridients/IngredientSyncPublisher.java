package com.example.restaurant.services.ingridients;

import com.example.restaurant.dto.sync.SyncIngredientResponse;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.helpers.staics.WebSocketEntityType;
import com.example.restaurant.helpers.staics.WebSocketTopics;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.services.NotificationServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientSyncPublisher {
  private final NotificationServices _notification;
  private final SyncMapper _syncMapper;

  public void publishIngredientsCreate(Ingredients ingredient) {
    WebSocketEvent<SyncIngredientResponse> event =
        WebSocketEvent.created(
            WebSocketEntityType.INGREDIENTS_ENTITY_TYPE,
            ingredient.getToken(),
            _syncMapper.toSyncIngredientResponse(ingredient));
    _notification.sendEventToTopic(WebSocketTopics.INGRIDIENTS_ADD_TOPIC, event);
  }

  public void publishIngredientsDelete(String token) {
    WebSocketEvent<Void> event =
        WebSocketEvent.deleted(WebSocketEntityType.INGREDIENTS_ENTITY_TYPE, token);

    _notification.sendEventToTopic(WebSocketTopics.INGRIDIENTS_REMOVE_TOPIC, event);
  }
}
