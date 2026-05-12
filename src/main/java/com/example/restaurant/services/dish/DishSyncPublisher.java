package com.example.restaurant.services.dish;

import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.dto.sync.SyncDishResponse;
import com.example.restaurant.helpers.WebSocketEvent;
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

  private static final String DISH_ENTITY_TYPE = "DISH";
  private static final String CATEGORY_ENTITY_TYPE = "DISH_CATEGORY";

  private static final String DISHES_TOPIC = "/menu/dishes";
  private static final String CATEGORIES_TOPIC = "/dictionary/dish-categories";

  public void publishDishCreated(Dishes dish) {
    sendDishEvent(dish, true);
  }

  public void publishDishUpdated(Dishes dish) {
    sendDishEvent(dish, false);
  }

  public void publishDishDeleted(String token) {
    WebSocketEvent<Void> event = WebSocketEvent.deleted(DISH_ENTITY_TYPE, token);
    _notification.sendEventToTopic(DISHES_TOPIC, event);
  }

  public void publishCategoryCreated(DishesCategories category) {
    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(category);
    WebSocketEvent<SyncDictionaryResponse> event =
        WebSocketEvent.created(CATEGORY_ENTITY_TYPE, category.getToken(), payload);
    _notification.sendEventToTopic(CATEGORIES_TOPIC, event);
  }

  public void publishCategoryDeleted(String token) {
    WebSocketEvent<Void> event = WebSocketEvent.deleted(CATEGORY_ENTITY_TYPE, token);
    _notification.sendEventToTopic(CATEGORIES_TOPIC, event);
  }

  private void sendDishEvent(Dishes dish, boolean isNew) {
    SyncDishResponse payload = _syncMapper.toSyncDishResponse(dish);
    WebSocketEvent<SyncDishResponse> event =
        isNew
            ? WebSocketEvent.created(DISH_ENTITY_TYPE, dish.getToken(), payload)
            : WebSocketEvent.updated(DISH_ENTITY_TYPE, dish.getToken(), payload);
    _notification.sendEventToTopic(DISHES_TOPIC, event);
  }
}
