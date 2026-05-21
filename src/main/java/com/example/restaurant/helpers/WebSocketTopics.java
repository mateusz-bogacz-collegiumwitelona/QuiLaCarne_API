package com.example.restaurant.helpers;

public final class WebSocketTopics {
  private WebSocketTopics() {}

  public static final String DICTIONARY_ALLERGENS = "/dictionary/allergens";
  public static final String DISHES_TOPIC = "/menu/dishes";
  public static final String CATEGORIES_TOPIC = "/dictionary/dish-categories";
  public static final String ORDERS_TOPIC = "/orders/updates";
  public static final String ITEMS_TOPIC = "/orders/items";
  public static final String ORDER_STATUS_TOPIC = "/dictionary/order-statuses";
  public static final String ITEM_STATUS_TOPIC = "/dictionary/order-item-statuses";
  public static final String RESERVATIONS_TOPIC = "/reservations/updates";
  public static final String PERSONNEL_TOPIC = "/personnel/updates";
  public static final String BAN_TOPIC = "/security/bans";
  public static final String TABLE_TOPIC = "/tables/updates";
  public static final String REPORTS_TOPIC = "/reports/updates";
}
