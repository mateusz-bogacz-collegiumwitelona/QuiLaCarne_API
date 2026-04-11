package com.example.restaurant.services;

import com.example.restaurant.dto.request.SyncRoleResponse;
import com.example.restaurant.dto.response.SyncBootstrapResponse;
import com.example.restaurant.dto.response.SyncDictionariesResponse;
import com.example.restaurant.dto.response.SyncDishResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

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
    @DisplayName("Calculate Page: Boundary Conditions")
    void calculatePage_InternalLogicCheck() {
        when(_roleRepo.count()).thenReturn(20L);
        SyncBootstrapResponse result = _syncServices.getBootstrapManifest();
        assertEquals(1, result.getModules().get("roles").getTotalPages());

        when(_ingredientsRepo.count()).thenReturn(21L);
        result = _syncServices.getBootstrapManifest();
        assertEquals(2, result.getModules().get("ingredients").getTotalPages());
    }

    @Test
    @DisplayName("Get Dictionaries: Should correctly map and return all dictionary lists")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getDictionaries_ShouldReturnMappedDictionaries() {
        BaseTranslatedEntity translatedEntity = mock(BaseTranslatedEntity.class);
        when(translatedEntity.getToken()).thenReturn("TOKEN_1");
        when(translatedEntity.getNameEn()).thenReturn("Apple");
        when(translatedEntity.getNamePl()).thenReturn("Jabłko");

        when(_allergenRepo.findAll()).thenReturn((List) List.of(translatedEntity));
        when(_ingredientsRepo.findAll()).thenReturn(List.of());
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
        assertTrue(response.getIngredients().isEmpty());
    }

    @Test
    @DisplayName("Get Dictionaries: Should throw exception for unsupported entity type")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getDictionaries_WhenUnsupportedEntity_ShouldThrowException() {
        BaseEntity unsupportedEntity = mock(BaseEntity.class);

        when(_allergenRepo.findAll()).thenReturn((List) List.of(unsupportedEntity));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> _syncServices.getDictionaries());

        assertTrue(exception.getMessage().contains("Entity type not supported for dictionary mapping"));
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
        ReflectionTestUtils.setField(_syncServices, "s3Endpoint", "https://s3.example.com");
        ReflectionTestUtils.setField(_syncServices, "s3BucketName", "my-restaurant");

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

        PagedResult<com.example.restaurant.dto.response.SyncDishResponse> result = _syncServices.getDishesSync(1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        com.example.restaurant.dto.response.SyncDishResponse response = result.getItems().getFirst();
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
}
