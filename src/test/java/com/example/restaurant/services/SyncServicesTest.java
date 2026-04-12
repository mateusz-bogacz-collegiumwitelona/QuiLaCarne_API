package com.example.restaurant.services;

import com.example.restaurant.dto.sync.*;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.*;
import com.example.restaurant.models.lookup.*;
import com.example.restaurant.repository.interfaces.*;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SyncServicesTest {
    @Mock
    private IUserRepository _userRepo;

    @Mock
    private IAllergensRepository _allergenRepo;

    @Mock
    private IBanRepository _banRepo;

    @Mock
    private IDishRepository _dishRepo;

    @Mock
    private IReportRepository _reportRepo;

    @Mock
    private IOrderRepository _orderRepo;

    @Mock
    private IReservationRepository _reservationRepo;

    @Mock
    private IRoleRepository _roleRepo;

    @Mock
    private ITableRespository _tableRepo;

    @Mock
    private IIngredientsRepository _ingredientsRepo;

    @InjectMocks
    private SyncServices _syncServices;

    @Spy
    private SyncMapper _syncMapper = Mappers.getMapper(SyncMapper.class);

    @Test
    @DisplayName("Get Bootstrap Manifest: Should return correct counts and calculated pages")
    void getBootstrapManifest_ShouldReturnCorrectData() {
        when(_dishRepo.count()).thenReturn(45L);
        when(_userRepo.count()).thenReturn(5L);
        when(_tableRepo.count()).thenReturn(0L);

        SyncBootstrapResponse result = _syncServices.getBootstrapManifest();

        assertNotNull(result);
        assertNotNull(result.getServerTime());
        Map<String, SyncBootstrapResponse.EntityMetadata> modules = result.getModules();

        assertEquals(45, modules.get("dishes").getTotalCount());
        assertEquals(3, modules.get("dishes").getTotalPages());

        assertEquals(5, modules.get("users").getTotalCount());
        assertEquals(1, modules.get("users").getTotalPages());

        assertEquals(0, modules.get("tables").getTotalCount());
        assertEquals(0, modules.get("tables").getTotalPages());

        assertTrue(modules.containsKey("roles"));
        assertTrue(modules.containsKey("orderStatuses"));
        assertTrue(modules.containsKey("reservations"));
    }

    @Test
    @DisplayName("Get Dictionaries: Should correctly map and return all dictionary lists")
    void getDictionaries_ShouldReturnMappedDictionaries() {
        Allergens allergen = new Allergens();
        allergen.setToken("TOKEN_1");
        allergen.setNameEn("Apple");
        allergen.setNamePl("Jabłko");

        when(_allergenRepo.findAll()).thenReturn(List.of(allergen));
        when(_dishRepo.findAllCategories()).thenReturn(List.of());
        when(_banRepo.findAllStatuses()).thenReturn(List.of());
        when(_reportRepo.findAllStatuses()).thenReturn(List.of());
        when(_orderRepo.findAllStatuses()).thenReturn(List.of());
        when(_orderRepo.findAllItemStatuses()).thenReturn(List.of());
        when(_reservationRepo.findAllStatuses()).thenReturn(List.of());
        when(_tableRepo.findAllStatuses()).thenReturn(List.of());

        SyncDictionariesResponse response = _syncServices.getDictionaries();

        assertNotNull(response);
        assertEquals(1, response.getAllergens().size());
        assertEquals("TOKEN_1", response.getAllergens().getFirst().getToken());
        assertEquals("Apple", response.getAllergens().getFirst().getNameEn());
        assertEquals("Jabłko", response.getAllergens().getFirst().getNamePl());
    }


    @Test
    @DisplayName("Get Roles: Should return mapped list of roles")
    void getRoles_ShouldReturnMappedRoles() {
        Roles role1 = mock(Roles.class);
        when(role1.getToken()).thenReturn("TOKEN_WAITER");
        when(role1.getName()).thenReturn("ROLE_WAITER");

        Roles role2 = mock(Roles.class);
        when(role2.getToken()).thenReturn("TOKEN_MANAGER");
        when(role2.getName()).thenReturn("ROLE_MANAGER");

        when(_roleRepo.findAll()).thenReturn(List.of(role1, role2));

        List<SyncRoleResponse> result = _syncServices.getRoles();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("TOKEN_WAITER", result.get(0).getToken());
        assertEquals("ROLE_WAITER", result.get(0).getName());
        assertEquals("TOKEN_MANAGER", result.get(1).getToken());
        assertEquals("ROLE_MANAGER", result.get(1).getName());
    }

    @Test
    @DisplayName("Get Roles: Should return empty list when no roles exist")
    void getRoles_ShouldReturnEmptyList() {
        when(_roleRepo.findAll()).thenReturn(List.of());

        List<SyncRoleResponse> result = _syncServices.getRoles();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Get Dishes Sync: Should correctly map dishes, format S3 URL and extract foreign keys")
    void getDishesSync_ShouldReturnMappedDishes() {
        ReflectionTestUtils.setField(_syncMapper, "s3Endpoint", "https://s3.example.com");
        ReflectionTestUtils.setField(_syncMapper, "s3BucketName", "my-restaurant");

        DishesCategories category = new DishesCategories();
        category.setToken("CAT_123");

        Ingredients ingredient = new Ingredients();
        ingredient.setToken("ING_456");

        Dishes dish = new Dishes();
        dish.setToken("DISH_789");
        dish.setName("Margherita");
        dish.setPrice(3500);
        dish.setAvailable(true);
        dish.setImageUrl("pizzas/margherita.jpg");
        dish.setCategory(category);
        dish.setIngredients(java.util.Set.of(ingredient));

        Page<Dishes> mockPage = new PageImpl<>(List.of(dish), PageRequest.of(0, 20), 1);
        when(_dishRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncDishResponse> result = _syncServices.getDishesSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        SyncDishResponse response = result.getItems().getFirst();
        assertEquals("DISH_789", response.getToken());
        assertEquals("Margherita", response.getName());
        assertEquals(3500, response.getPrice());
        assertTrue(response.isAvailable());

        assertEquals("CAT_123", response.getCategoryToken());
        assertEquals(1, response.getIngredientTokens().size());
        assertEquals("ING_456", response.getIngredientTokens().getFirst());

        assertEquals("https://s3.example.com/my-restaurant/pizzas/margherita.jpg", response.getImageUrl());
    }

    @Test
    @DisplayName("Get Dishes Sync: Should handle missing relations and absolute URLs safely")
    void getDishesSync_ShouldHandleNullsAndAbsoluteUrls() {
        Dishes dish = new Dishes();
        dish.setToken("DISH_NULL");
        dish.setImageUrl("http://external-domain.com/image.png");

        Page<Dishes> mockPage = new PageImpl<>(List.of(dish), PageRequest.of(0, 20), 1);
        when(_dishRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncDishResponse> result = _syncServices.getDishesSync(0);

        assertNotNull(result);
        SyncDishResponse response = result.getItems().getFirst();

        assertNull(response.getCategoryToken());
        assertNotNull(response.getIngredientTokens());
        assertTrue(response.getIngredientTokens().isEmpty());

        assertEquals("http://external-domain.com/image.png", response.getImageUrl());
    }

    @Test
    @DisplayName("Get Bans Sync: Should correctly map bans and extract foreign keys")
    void getBansSync_ShouldReturnMappedBans() {
        Users user = new Users();
        user.setToken("USER_123");

        Users admin = new Users();
        admin.setToken("ADMIN_456");

        BanStatus status = new BanStatus();
        status.setToken("STATUS_ACTIVE");

        Bans ban = new Bans();
        ban.setToken("BAN_789");
        ban.setUser(user);
        ban.setBannedBy(admin);
        ban.setReason("Złamanie regulaminu");
        ban.setIsActive(true);
        ban.setBanStatuses(Set.of(status));

        Page<Bans> mockPage =
                new PageImpl<>(
                        List.of(ban),
                        PageRequest.of(0, 20),
                        1
                );
        when(_banRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncBanResponse> result =
                _syncServices.getBansSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        SyncBanResponse response = result.getItems().getFirst();
        assertEquals("BAN_789", response.getToken());
        assertEquals("Złamanie regulaminu", response.getReason());
        assertTrue(response.getIsActive());

        assertEquals("USER_123", response.getUserToken());
        assertEquals("ADMIN_456", response.getBannedByToken());
        assertEquals(1, response.getStatusTokens().size());
        assertEquals("STATUS_ACTIVE", response.getStatusTokens().getFirst());
    }

    @Test
    @DisplayName("Get Bans Sync: Should handle missing relations gracefully")
    void getBansSync_ShouldHandleNullRelations() {
        Bans ban = new Bans();
        ban.setToken("BAN_NULL_TEST");

        Page<Bans> mockPage =
                new PageImpl<>(
                        List.of(ban),
                        PageRequest.of(0, 20),
                        1
                );
        when(_banRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncBanResponse> result =
                _syncServices.getBansSync(1);

        assertNotNull(result);
        SyncBanResponse response = result.getItems().getFirst();

        assertNull(response.getUserToken());
        assertNull(response.getBannedByToken());
        assertNotNull(response.getStatusTokens());
        assertTrue(response.getStatusTokens().isEmpty());
    }

    @Test
    @DisplayName("Get Reports Sync: Should correctly map reports and extract foreign keys")
    void getReportsSync_ShouldReturnMappedReports() {
        GuestReports report = getGuestReports();

        Page<GuestReports> mockPage =
                new PageImpl<>(
                        List.of(report),
                        PageRequest.of(0, 20),
                        1
                );

        when(_reportRepo.findAll(ArgumentMatchers.isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        PagedResult<SyncReportResponse> result =
                _syncServices.getReportsSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        SyncReportResponse response = result.getItems().getFirst();
        assertEquals("REPORT_789", response.getToken());
        assertEquals("Głośne zachowanie", response.getReason());
        assertEquals("GUEST_123", response.getGuestToken());
        assertEquals("REPORTER_456", response.getReporterToken());
        assertEquals(1, response.getStatusTokens().size());
        assertEquals("STATUS_IN_PROGRESS", response.getStatusTokens().getFirst());
    }


    @Test
    @DisplayName("Get Reports Sync: Should handle missing relations gracefully")
    void getReportsSync_ShouldHandleNullRelations() {
        GuestReports report = new GuestReports();
        report.setToken("REPORT_NULL_TEST");

        Page<GuestReports> mockPage =
                new PageImpl<>(
                        List.of(report),
                        PageRequest.of(0, 20),
                        1
                );

        when(_reportRepo.findAll(ArgumentMatchers.isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        PagedResult<SyncReportResponse> result =
                _syncServices.getReportsSync(1);

        assertNotNull(result);
        SyncReportResponse response = result.getItems().getFirst();

        assertNull(response.getGuestToken());
        assertNull(response.getReporterToken());
        assertNotNull(response.getStatusTokens());
        assertTrue(response.getStatusTokens().isEmpty());
    }

    @Test
    @DisplayName("Get Ingredients Sync: Should correctly map ingredients and extract allergen tokens")
    void getIngredientsSync_ShouldReturnMappedIngredients() {
        Allergens allergen = new Allergens();
        allergen.setToken("ALLERGEN_PEANUTS");

        Ingredients ingredient = new Ingredients();
        ingredient.setToken("ING_PEANUT_BUTTER");
        ingredient.setNameEn("Peanut Butter");
        ingredient.setNamePl("Masło Orzechowe");
        ingredient.setAllergens(java.util.Set.of(allergen));

        Page<Ingredients> mockPage =
                new PageImpl<>(
                        List.of(ingredient),
                        PageRequest.of(0, 20),
                        1
                );
        when(_ingredientsRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncIngredientResponse> result =
                _syncServices.getIngredientsSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        SyncIngredientResponse response = result.getItems().getFirst();
        assertEquals("ING_PEANUT_BUTTER", response.getToken());
        assertEquals("Peanut Butter", response.getNameEn());
        assertEquals("Masło Orzechowe", response.getNamePl());
        assertNotNull(response.getAllergenTokens());
        assertEquals(1, response.getAllergenTokens().size());
        assertEquals("ALLERGEN_PEANUTS", response.getAllergenTokens().getFirst());
    }

    @Test
    @DisplayName("Get Ingredients Sync: Should handle missing relations gracefully")
    void getIngredientsSync_ShouldHandleNullRelations() {
        Ingredients ingredient = new Ingredients();
        ingredient.setToken("ING_WATER");
        ingredient.setNameEn("Water");
        ingredient.setNamePl("Woda");

        Page<Ingredients> mockPage =
                new PageImpl<>(
                        List.of(ingredient),
                        PageRequest.of(0, 20),
                        1
                );
        when(_ingredientsRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncIngredientResponse> result =
                _syncServices.getIngredientsSync(1);

        assertNotNull(result);
        SyncIngredientResponse response = result.getItems().getFirst();

        assertNotNull(response.getAllergenTokens());
        assertTrue(response.getAllergenTokens().isEmpty());
    }

    @Test
    @DisplayName("Get Orders Sync: Should correctly map orders and extract foreign keys")
    void getOrdersSync_ShouldReturnMappedOrders() {
        Orders order = getOrders();

        Page<Orders> mockPage = new PageImpl<>(
                List.of(order),
                PageRequest.of(0, 20),
                1
        );
        when(_orderRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncOrderResponse> result = _syncServices.getOrdersSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        SyncOrderResponse response = result.getItems().getFirst();
        assertEquals("ORDER_1", response.getToken());
        assertEquals(15000, response.getTotalPrice());

        assertEquals("RES_123", response.getReservationToken());
        assertEquals("TABLE_5", response.getTableToken());
        assertEquals("WAITER_99", response.getWaiterToken());
        assertEquals(1, response.getStatusTokens().size());
        assertEquals("STATUS_NEW", response.getStatusTokens().getFirst());
    }

    @Test
    @DisplayName("Get Orders Sync: Should handle missing relations gracefully")
    void getOrdersSync_ShouldHandleNullRelations() {
        Orders order = new Orders();
        order.setToken("ORDER_NULL");

        Page<Orders> mockPage = new PageImpl<>(
                List.of(order),
                PageRequest.of(0, 20),
                1
        );
        when(_orderRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncOrderResponse> result = _syncServices.getOrdersSync(1);

        assertNotNull(result);
        SyncOrderResponse response = result.getItems().getFirst();

        assertNull(response.getReservationToken());
        assertNull(response.getTableToken());
        assertNull(response.getWaiterToken());
        assertNotNull(response.getStatusTokens());
        assertTrue(response.getStatusTokens().isEmpty());
    }

    @Test
    @DisplayName("Get Order Items Sync: Should correctly map items and extract foreign keys")
    void getOrderItemsSync_ShouldReturnMappedItems() {
        OrderItems item = getOrderItems();

        Page<OrderItems> mockPage = new PageImpl<>(
                List.of(item),
                PageRequest.of(0, 20),
                1
        );
        when(_orderRepo.findAllItems(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncOrderItemResponse> result = _syncServices.getOrderItemsSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        SyncOrderItemResponse response = result.getItems().getFirst();
        assertEquals("ITEM_1", response.getToken());
        assertEquals(2, response.getQuantity());
        assertEquals(3500, response.getPriceAtTimeOfOrder());
        assertEquals("Bez cebuli", response.getNote());

        assertEquals("ORDER_1", response.getOrderToken());
        assertEquals("DISH_PIZZA", response.getProductToken());
        assertEquals(1, response.getStatusTokens().size());
        assertEquals("ITEM_STATUS_DONE", response.getStatusTokens().getFirst());
    }

    private static @NonNull OrderItems getOrderItems() {
        Orders order = new Orders();
        order.setToken("ORDER_1");

        Dishes dish = new Dishes();
        dish.setToken("DISH_PIZZA");

        OrderItemsStatus status = new OrderItemsStatus();
        status.setToken("ITEM_STATUS_DONE");

        OrderItems item = new OrderItems();
        item.setToken("ITEM_1");
        item.setOrder(order);
        item.setProduct(dish);
        item.setQuantity(2);
        item.setPriceAtTimeOfOrder(3500);
        item.setNote("Bez cebuli");
        item.setStatuses(Set.of(status));
        return item;
    }

    @Test
    @DisplayName("Get Order Items Sync: Should handle missing relations gracefully")
    void getOrderItemsSync_ShouldHandleNullRelations() {
        OrderItems item = new OrderItems();
        item.setToken("ITEM_NULL");

        Page<OrderItems> mockPage = new PageImpl<>(
                List.of(item),
                PageRequest.of(0, 20),
                1
        );
        when(_orderRepo.findAllItems(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncOrderItemResponse> result = _syncServices.getOrderItemsSync(1);

        assertNotNull(result);
        SyncOrderItemResponse response = result.getItems().getFirst();

        assertNull(response.getOrderToken());
        assertNull(response.getProductToken());
        assertNotNull(response.getStatusTokens());
        assertTrue(response.getStatusTokens().isEmpty());
    }

    @Test
    @DisplayName("Get Reservations Sync: Should correctly map reservations and extract foreign keys")
    void getReservationsSync_ShouldReturnMappedReservations() {
        Users user = new Users();
        user.setToken("USER_1");

        RestaurantTables table = new RestaurantTables();
        table.setToken("TABLE_10");

        ReservationStatus status = new ReservationStatus();
        status.setToken("RES_STATUS_CONFIRMED");

        Reservations reservation = new Reservations();
        reservation.setToken("RES_1");
        reservation.setUser(user);
        reservation.setTableId(table);
        reservation.setReservationStatus(Set.of(status));
        reservation.setStartTime(OffsetDateTime.now());
        reservation.setEndTime(OffsetDateTime.now().plusHours(2));

        Page<Reservations> mockPage = new PageImpl<>(
                List.of(reservation),
                PageRequest.of(0, 20),
                1
        );
        when(_reservationRepo.findAll(ArgumentMatchers.isNull(), any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncReservationResponse> result = _syncServices.getReservationsSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        SyncReservationResponse response = result.getItems().getFirst();
        assertEquals("RES_1", response.getToken());
        assertNotNull(response.getStartTime());
        assertNotNull(response.getEndTime());

        assertEquals("USER_1", response.getUserToken());
        assertEquals("TABLE_10", response.getTableToken());
        assertEquals(1, response.getStatusTokens().size());
        assertEquals("RES_STATUS_CONFIRMED", response.getStatusTokens().getFirst());
    }

    @Test
    @DisplayName("Get Reservations Sync: Should handle missing relations gracefully")
    void getReservationsSync_ShouldHandleNullRelations() {
        Reservations reservation = new Reservations();
        reservation.setToken("RES_NULL");

        Page<Reservations> mockPage = new PageImpl<>(
                List.of(reservation),
                PageRequest.of(0, 20),
                1
        );
        when(_reservationRepo.findAll(ArgumentMatchers.isNull(), any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncReservationResponse> result = _syncServices.getReservationsSync(1);

        assertNotNull(result);
        SyncReservationResponse response = result.getItems().getFirst();

        assertNull(response.getUserToken());
        assertNull(response.getTableToken());
        assertNotNull(response.getStatusTokens());
        assertTrue(response.getStatusTokens().isEmpty());
    }

    private static @NonNull GuestReports getGuestReports() {
        Users guest = new Users();
        guest.setToken("GUEST_123");

        Users reporter = new Users();
        reporter.setToken("REPORTER_456");

        GuestReportStatus status = new GuestReportStatus();
        status.setToken("STATUS_IN_PROGRESS");

        GuestReports report = new GuestReports();
        report.setToken("REPORT_789");
        report.setGuest(guest);
        report.setReporter(reporter);
        report.setReason("Głośne zachowanie");
        report.setStatuses(Set.of(status));
        return report;
    }

    private static @NonNull Orders getOrders() {
        Reservations reservation = new Reservations();
        reservation.setToken("RES_123");

        RestaurantTables table = new RestaurantTables();
        table.setToken("TABLE_5");

        Users waiter = new Users();
        waiter.setToken("WAITER_99");

        OrderStatus status = new OrderStatus();
        status.setToken("STATUS_NEW");

        Orders order = new Orders();
        order.setToken("ORDER_1");
        order.setReservation(reservation);
        order.setTable(table);
        order.setWaiter(waiter);
        order.setTotalPrice(15000);
        order.setStatuses(Set.of(status));
        return order;
    }

    @Test
    @DisplayName("Get Tables Sync: Should correctly map tables and extract foreign keys")
    void getTablesSync_ShouldReturnMappedTables() {
        TableStatus status = new TableStatus();
        status.setToken("TABLE_STATUS_FREE");

        RestaurantTables table = new RestaurantTables();
        table.setToken("TABLE_1");
        table.setTableNumber(5);
        table.setCapacity(4);
        table.setTableStatus(Set.of(status));

        Page<RestaurantTables> mockPage = new PageImpl<>(
                List.of(table),
                PageRequest.of(0, 20),
                1
        );
        when(_tableRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncTableResponse> result = _syncServices.getTablesSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        SyncTableResponse response = result.getItems().getFirst();
        assertEquals("TABLE_1", response.getToken());
        assertEquals(5, response.getTableNumber());
        assertEquals(4, response.getCapacity());

        assertEquals(1, response.getStatusTokens().size());
        assertEquals("TABLE_STATUS_FREE", response.getStatusTokens().getFirst());
    }

    @Test
    @DisplayName("Get Tables Sync: Should handle missing relations gracefully")
    void getTablesSync_ShouldHandleNullRelations() {
        RestaurantTables table = new RestaurantTables();
        table.setToken("TABLE_NULL");

        Page<RestaurantTables> mockPage = new PageImpl<>(
                List.of(table),
                PageRequest.of(0, 20),
                1
        );
        when(_tableRepo.findAll(any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncTableResponse> result = _syncServices.getTablesSync(1);

        assertNotNull(result);
        SyncTableResponse response = result.getItems().getFirst();

        assertNotNull(response.getStatusTokens());
        assertTrue(response.getStatusTokens().isEmpty());
    }

    @Test
    @DisplayName("Get Users Sync: Should correctly map users, evaluate isStaff flag, and extract role tokens")
    void getUsersSync_ShouldReturnMappedUsers() {
        Roles waiterRole = new Roles();
        waiterRole.setToken("ROLE_WAITER_TOKEN");
        waiterRole.setName("ROLE_WAITER");

        Users user = new Users();
        user.setToken("USER_1");
        user.setUsername("jankowalski");
        user.setEmail("jan@example.com");
        user.setIsActive(true);
        user.setRoles(Set.of(waiterRole));

        Page<Users> mockPage = new PageImpl<>(
                List.of(user),
                PageRequest.of(0, 20),
                1
        );
        when(_userRepo.findAllUsers(ArgumentMatchers.isNull(), any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncUserResponse> result = _syncServices.getUsersSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        SyncUserResponse response = result.getItems().getFirst();
        assertEquals("USER_1", response.getToken());
        assertEquals("jankowalski", response.getUsername());
        assertEquals("jan@example.com", response.getEmail());
        assertTrue(response.getIsActive());
        assertTrue(response.isStaff());

        assertEquals(1, response.getRoleTokens().size());
        assertEquals("ROLE_WAITER_TOKEN", response.getRoleTokens().getFirst());
    }

    @Test
    @DisplayName("Get Users Sync: Should set isStaff to false for pure guests and handle missing relations")
    void getUsersSync_ShouldHandleGuestsAndNullRelations() {
        Roles guestRole = new Roles();
        guestRole.setToken("ROLE_GUEST_TOKEN");
        guestRole.setName("ROLE_GUEST");

        Users user = new Users();
        user.setToken("GUEST_USER");
        user.setRoles(Set.of(guestRole));

        Page<Users> mockPage = new PageImpl<>(
                List.of(user),
                PageRequest.of(0, 20),
                1
        );
        when(_userRepo.findAllUsers(ArgumentMatchers.isNull(), any(Pageable.class))).thenReturn(mockPage);

        PagedResult<SyncUserResponse> result = _syncServices.getUsersSync(1);

        assertNotNull(result);
        SyncUserResponse response = result.getItems().getFirst();

        assertFalse(response.isStaff());
        assertNotNull(response.getRoleTokens());
        assertFalse(response.getRoleTokens().isEmpty());
    }
}
