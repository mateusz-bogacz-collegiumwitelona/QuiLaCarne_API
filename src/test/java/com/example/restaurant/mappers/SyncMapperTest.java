package com.example.restaurant.mappers;

import static org.junit.jupiter.api.Assertions.*;

import com.example.restaurant.dto.sync.*;
import com.example.restaurant.models.*;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.models.lookup.Roles;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

class SyncMapperTest {

  private SyncMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(SyncMapper.class);
    ReflectionTestUtils.setField(mapper, "s3Endpoint", "https://s3.eu-central-1.amazonaws.com");
    ReflectionTestUtils.setField(mapper, "s3BucketName", "restaurant-assets");
  }

  @Test
  @DisplayName("User Mapping: Should identify staff and map role tokens")
  void toSyncUserResponse_ShouldMapCorrectly() {
    Roles waiterRole = new Roles();
    waiterRole.setToken("ROLE_WAITER_TOKEN");
    waiterRole.setName("ROLE_WAITER");

    Users user = new Users();
    user.setToken("USER_TOKEN");
    user.setRoles(Set.of(waiterRole));

    SyncUserResponse result = mapper.toSyncUserResponse(user);

    assertTrue(result.isStaff(), "User with ROLE_WAITER should be identified as staff");
    assertEquals(1, result.getRoleTokens().size());
    assertEquals("ROLE_WAITER_TOKEN", result.getRoleTokens().getFirst());
  }

  @Test
  @DisplayName("User Mapping: Should set isStaff to false for non-staff roles")
  void toSyncUserResponse_ShouldHandleNonStaff() {
    Roles clientRole = new Roles();
    clientRole.setName("ROLE_CLIENT");

    Users user = new Users();
    user.setRoles(Set.of(clientRole));

    SyncUserResponse result = mapper.toSyncUserResponse(user);

    assertFalse(result.isStaff(), "Regular client should not be identified as staff");
  }

  @Test
  @DisplayName("Dish Mapping: Should prefix relative image URL with S3 path")
  void toSyncDishResponse_ShouldPrefixRelativeImageUrl() {
    Dishes dish = new Dishes();
    dish.setImageUrl("pizzas/margherita.png");

    SyncDishResponse result = mapper.toSyncDishResponse(dish);

    String expectedUrl =
        "https://s3.eu-central-1.amazonaws.com/restaurant-assets/pizzas/margherita.png";
    assertEquals(expectedUrl, result.getImageUrl(), "Relative URL should be prefixed");
  }

  @Test
  @DisplayName("Dish Mapping: Should NOT prefix absolute image URLs")
  void toSyncDishResponse_ShouldNotPrefixAbsoluteUrl() {
    String absoluteUrl = "https://other-site.com/image.jpg";
    Dishes dish = new Dishes();
    dish.setImageUrl(absoluteUrl);

    SyncDishResponse result = mapper.toSyncDishResponse(dish);

    assertEquals(
        absoluteUrl,
        result.getImageUrl(),
        "Absolute URL starting with http should remain unchanged");
  }

  @Test
  @DisplayName("Order Mapping: Should map complex relations to flat tokens")
  void toSyncOrderResponse_ShouldMapRelationTokens() {
    Users waiter = new Users();
    waiter.setToken("WAITER_TOKEN");

    RestaurantTables table = new RestaurantTables();
    table.setToken("TABLE_TOKEN");

    Orders order = new Orders();
    order.setToken("ORDER_TOKEN");
    order.setWaiter(waiter);
    order.setTable(table);

    SyncOrderResponse result = mapper.toSyncOrderResponse(order);

    assertEquals("WAITER_TOKEN", result.getWaiterToken());
    assertEquals("TABLE_TOKEN", result.getTableToken());
    assertEquals("ORDER_TOKEN", result.getToken());
  }

  @Test
  @DisplayName("Ingredient Mapping: Should map allergen tokens or return empty list")
  void toSyncIngredientResponse_ShouldMapAllergens() {
    Allergens nuts = new Allergens();
    nuts.setToken("ALL_NUTS");

    Ingredients ingredient = new Ingredients();
    ingredient.setAllergens(Set.of(nuts));

    SyncIngredientResponse result = mapper.toSyncIngredientResponse(ingredient);

    assertEquals(1, result.getAllergenTokens().size());
    assertEquals("ALL_NUTS", result.getAllergenTokens().getFirst());
    ingredient.setAllergens(null);
    SyncIngredientResponse resultNull = mapper.toSyncIngredientResponse(ingredient);
    assertNotNull(resultNull.getAllergenTokens(), "Should return empty list instead of null");
  }

  @Test
  @DisplayName("Reservation Mapping: Should map tableId to tableToken correctly")
  void toSyncReservationResponse_ShouldMapTableToken() {
    RestaurantTables table = new RestaurantTables();
    table.setToken("TABLE_5");

    Reservations res = new Reservations();
    res.setTableId(table);

    SyncReservationResponse result = mapper.toSyncReservationResponse(res);

    assertEquals("TABLE_5", result.getTableToken(), "Table object should be mapped to its token");
  }

  @Test
  @DisplayName("Staff Check: Logic should cover all staff roles")
  void checkIfStaff_ShouldReturnTrue_ForAllStaffRoles() {
    Roles admin = new Roles();
    admin.setName("ROLE_ADMIN");
    Roles manager = new Roles();
    manager.setName("ROLE_MANAGER");
    Roles waiter = new Roles();
    waiter.setName("ROLE_WAITER");

    assertTrue(mapper.toSyncUserResponse(createUserWithRole(admin)).isStaff());
    assertTrue(mapper.toSyncUserResponse(createUserWithRole(manager)).isStaff());
    assertTrue(mapper.toSyncUserResponse(createUserWithRole(waiter)).isStaff());
  }

  private Users createUserWithRole(Roles role) {
    Users user = new Users();
    user.setRoles(Set.of(role));
    return user;
  }
}
