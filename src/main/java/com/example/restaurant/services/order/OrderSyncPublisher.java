package com.example.restaurant.services.order;

import static com.example.restaurant.helpers.staics.WebSocketTopics.ITEMS_TOPIC;

import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.dto.sync.SyncOrderItemResponse;
import com.example.restaurant.dto.sync.SyncOrderResponse;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.helpers.staics.WebSocketEntityType;
import com.example.restaurant.helpers.staics.WebSocketTopics;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.services.NotificationServices;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSyncPublisher {
  private final NotificationServices _notification;
  private final SyncMapper _syncMapper;

  public void publishOrderCreated(Orders order, List<OrderItems> items) {
    sendOrderEvent(order, true);
    sendItemEvents(items, true);
  }

  public void publishOrderUpdated(Orders order, List<OrderItems> items) {
    sendOrderEvent(order, false);
    if (items != null && !items.isEmpty()) {
      sendItemEvents(items, false);
    }
  }

  public void publishOrderDeleted(String orderToken) {
    _notification.sendEventToTopic(
        WebSocketTopics.ORDERS_TOPIC,
        WebSocketEvent.deleted(WebSocketEntityType.ORDER_ENTITY_TYPE, orderToken));
  }

  public void publishOrderStatusChange(BaseTranslatedEntity status, boolean isCreated) {
    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(status);
    WebSocketEvent<SyncDictionaryResponse> event =
        isCreated
            ? WebSocketEvent.created(
                WebSocketEntityType.ORDER_STATUS_ENTITY_TYPE, status.getToken(), payload)
            : WebSocketEvent.updated(
                WebSocketEntityType.ORDER_STATUS_ENTITY_TYPE, status.getToken(), payload);
    _notification.sendEventToTopic(WebSocketTopics.ORDER_STATUS_TOPIC, event);
  }

  public void publishOrderStatusDeleted(String token) {
    _notification.sendEventToTopic(
        WebSocketTopics.ORDER_STATUS_TOPIC,
        WebSocketEvent.deleted(WebSocketEntityType.ORDER_STATUS_ENTITY_TYPE, token));
  }

  public void publishOrderItemStatusChange(BaseTranslatedEntity status, boolean isCreated) {
    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(status);
    WebSocketEvent<SyncDictionaryResponse> event =
        isCreated
            ? WebSocketEvent.created(
                WebSocketEntityType.ORDER_ITEM_STATUS_ENTITY_TYPE, status.getToken(), payload)
            : WebSocketEvent.updated(
                WebSocketEntityType.ORDER_ITEM_STATUS_ENTITY_TYPE, status.getToken(), payload);
    _notification.sendEventToTopic(WebSocketTopics.ITEM_STATUS_TOPIC, event);
  }

  public void publishOrderItemStatusDeleted(String token) {
    _notification.sendEventToTopic(
        WebSocketTopics.ITEM_STATUS_TOPIC,
        WebSocketEvent.deleted(WebSocketEntityType.ORDER_ITEM_STATUS_ENTITY_TYPE, token));
  }

  private void sendOrderEvent(Orders order, boolean isNew) {
    SyncOrderResponse payload = _syncMapper.toSyncOrderResponse(order);
    WebSocketEvent<SyncOrderResponse> event =
        isNew
            ? WebSocketEvent.created(
                WebSocketEntityType.ORDER_ENTITY_TYPE, order.getToken(), payload)
            : WebSocketEvent.updated(
                WebSocketEntityType.ORDER_ENTITY_TYPE, order.getToken(), payload);
    _notification.sendEventToTopic(WebSocketTopics.ORDERS_TOPIC, event);
  }

  private void sendItemEvents(List<OrderItems> items, boolean isNew) {
    for (OrderItems item : items) {
      SyncOrderItemResponse payload = _syncMapper.toSyncOrderItemResponse(item);
      WebSocketEvent<SyncOrderItemResponse> event =
          isNew
              ? WebSocketEvent.created(
                  WebSocketEntityType.ORDER_ITEM_ENTITY_TYPE, item.getToken(), payload)
              : WebSocketEvent.updated(
                  WebSocketEntityType.ORDER_ITEM_ENTITY_TYPE, item.getToken(), payload);
      _notification.sendEventToTopic(ITEMS_TOPIC, event);
    }
  }
}
