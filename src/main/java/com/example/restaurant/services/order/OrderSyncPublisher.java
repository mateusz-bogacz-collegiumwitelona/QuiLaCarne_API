package com.example.restaurant.services.order;

import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.dto.sync.SyncOrderItemResponse;
import com.example.restaurant.dto.sync.SyncOrderResponse;
import com.example.restaurant.helpers.WebSocketEvent;
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

  private static final String ORDER_ENTITY_TYPE = "ORDER";
  private static final String ORDER_ITEM_ENTITY_TYPE = "ORDER_ITEM";
  private static final String ORDER_STATUS_ENTITY_TYPE = "ORDER_STATUS";
  private static final String ORDER_ITEM_STATUS_ENTITY_TYPE = "ORDER_ITEM_STATUS";

  private static final String ORDERS_TOPIC = "/orders/updates";
  private static final String ITEMS_TOPIC = "/orders/items";
  private static final String ORDER_STATUS_TOPIC = "/dictionary/order-statuses";
  private static final String ITEM_STATUS_TOPIC = "/dictionary/order-item-statuses";

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
        ORDERS_TOPIC, WebSocketEvent.deleted(ORDER_ENTITY_TYPE, orderToken));
  }

  public void publishOrderStatusChange(BaseTranslatedEntity status, boolean isCreated) {
    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(status);
    WebSocketEvent<SyncDictionaryResponse> event =
        isCreated
            ? WebSocketEvent.created(ORDER_STATUS_ENTITY_TYPE, status.getToken(), payload)
            : WebSocketEvent.updated(ORDER_STATUS_ENTITY_TYPE, status.getToken(), payload);
    _notification.sendEventToTopic(ORDER_STATUS_TOPIC, event);
  }

  public void publishOrderStatusDeleted(String token) {
    _notification.sendEventToTopic(
        ORDER_STATUS_TOPIC, WebSocketEvent.deleted(ORDER_STATUS_ENTITY_TYPE, token));
  }

  public void publishOrderItemStatusChange(BaseTranslatedEntity status, boolean isCreated) {
    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(status);
    WebSocketEvent<SyncDictionaryResponse> event =
        isCreated
            ? WebSocketEvent.created(ORDER_ITEM_STATUS_ENTITY_TYPE, status.getToken(), payload)
            : WebSocketEvent.updated(ORDER_ITEM_STATUS_ENTITY_TYPE, status.getToken(), payload);
    _notification.sendEventToTopic(ITEM_STATUS_TOPIC, event);
  }

  public void publishOrderItemStatusDeleted(String token) {
    _notification.sendEventToTopic(
        ITEM_STATUS_TOPIC, WebSocketEvent.deleted(ORDER_ITEM_STATUS_ENTITY_TYPE, token));
  }

  private void sendOrderEvent(Orders order, boolean isNew) {
    SyncOrderResponse payload = _syncMapper.toSyncOrderResponse(order);
    WebSocketEvent<SyncOrderResponse> event =
        isNew
            ? WebSocketEvent.created(ORDER_ENTITY_TYPE, order.getToken(), payload)
            : WebSocketEvent.updated(ORDER_ENTITY_TYPE, order.getToken(), payload);
    _notification.sendEventToTopic(ORDERS_TOPIC, event);
  }

  private void sendItemEvents(List<OrderItems> items, boolean isNew) {
    for (OrderItems item : items) {
      SyncOrderItemResponse payload = _syncMapper.toSyncOrderItemResponse(item);
      WebSocketEvent<SyncOrderItemResponse> event =
          isNew
              ? WebSocketEvent.created(ORDER_ITEM_ENTITY_TYPE, item.getToken(), payload)
              : WebSocketEvent.updated(ORDER_ITEM_ENTITY_TYPE, item.getToken(), payload);
      _notification.sendEventToTopic(ITEMS_TOPIC, event);
    }
  }
}
