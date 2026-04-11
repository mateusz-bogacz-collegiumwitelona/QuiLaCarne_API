package com.example.restaurant.services;

import com.example.restaurant.dto.response.SyncBootstrapResponse;
import com.example.restaurant.repository.interfaces.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
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
}
