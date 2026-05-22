package com.example.restaurant.services.dish;

import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.dto.sync.SyncDishResponse;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.helpers.staics.WebSocketEntityType;
import com.example.restaurant.helpers.staics.WebSocketTopics;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.services.NotificationServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DishSyncPublisher {
  private final NotificationServices _notification;
  private final SyncMapper _syncMapper;

  public void publishDishCreated(Dishes dish) {
    sendDishEvent(dish, true);
  }

  public void publishDishUpdated(Dishes dish) {
    sendDishEvent(dish, false);
  }

  public void publishDishDeleted(String token) {
    WebSocketEvent<Void> event =
        WebSocketEvent.deleted(WebSocketEntityType.DISH_ENTITY_TYPE, token);
    _notification.sendEventToTopic(WebSocketTopics.DISHES_TOPIC, event);
  }

  public void publishCategoryCreated(DishesCategories category) {
    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(category);
    WebSocketEvent<SyncDictionaryResponse> event =
        WebSocketEvent.created(
            WebSocketEntityType.CATEGORY_ENTITY_TYPE, category.getToken(), payload);
    _notification.sendEventToTopic(WebSocketTopics.CATEGORIES_TOPIC, event);
  }

  public void publishCategoryDeleted(String token) {
    WebSocketEvent<Void> event =
        WebSocketEvent.deleted(WebSocketEntityType.CATEGORY_ENTITY_TYPE, token);
    _notification.sendEventToTopic(WebSocketTopics.CATEGORIES_TOPIC, event);
  }

  private void sendDishEvent(Dishes dish, boolean isNew) {
    SyncDishResponse payload = _syncMapper.toSyncDishResponse(dish);
    WebSocketEvent<SyncDishResponse> event =
        isNew
            ? WebSocketEvent.created(WebSocketEntityType.DISH_ENTITY_TYPE, dish.getToken(), payload)
            : WebSocketEvent.updated(
                WebSocketEntityType.DISH_ENTITY_TYPE, dish.getToken(), payload);
    _notification.sendEventToTopic(WebSocketTopics.DISHES_TOPIC, event);
  }
}
