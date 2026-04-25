package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.sync.*;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.*;
import com.example.restaurant.repository.interfaces.*;
import com.example.restaurant.services.interfaces.ISyncServices;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SyncServices implements ISyncServices {
    private final IUserRepository _userRepo;
    private final IAllergensRepository _allergenRepo;
    private final IBanRepository _banRepo;
    private final IDishRepository _dishRepo;
    private final IReportRepository _reportRepo;
    private final IOrderRepository _orderRepo;
    private final IReservationRepository _reservationRepo;
    private final IRoleRepository _roleRepo;
    private final ITableRespository _tableRepo;
    private final IIngredientsRepository _ingredientsRepo;

    private final SyncMapper _syncMapper;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    @Auditable(action = "BOOTSTRAP_MANIFEST")
    public SyncBootstrapResponse getBootstrapManifest() {
        Map<String, SyncBootstrapResponse.EntityMetadata> modules = new HashMap<>();

        addModuleMetadata(modules, "roles", _roleRepo.count());
        addModuleMetadata(modules, "allergens", _allergenRepo.count());
        addModuleMetadata(modules, "dishCategories", _dishRepo.countCategories());

        addModuleMetadata(modules, "banStatuses", _banRepo.countStatuses());
        addModuleMetadata(modules, "reportStatuses", _reportRepo.countStatuses());
        addModuleMetadata(modules, "orderStatuses", _orderRepo.countStatuses());
        addModuleMetadata(modules, "orderItemStatuses", _orderRepo.countOrderItemsStatuses());
        addModuleMetadata(modules, "reservationStatuses", _reservationRepo.countStatuses());
        addModuleMetadata(modules, "tableStatuses", _tableRepo.countStatuses());

        addModuleMetadata(modules, "users", _userRepo.count());
        addModuleMetadata(modules, "dishes", _dishRepo.count());
        addModuleMetadata(modules, "tables", _tableRepo.count());

        addModuleMetadata(modules, "bans", _banRepo.count());
        addModuleMetadata(modules, "reports", _reportRepo.count());
        addModuleMetadata(modules, "orders", _orderRepo.count());
        addModuleMetadata(modules, "orderItems", _orderRepo.countItems());
        addModuleMetadata(modules, "reservations", _reservationRepo.count());

        return SyncBootstrapResponse.builder()
                .modules(modules)
                .serverTime(OffsetDateTime.now())
                .build();
    }

    @Override
    @Auditable(action = "GET_DICTIONARIES")
    public SyncDictionariesResponse getDictionaries() {
        return SyncDictionariesResponse.builder()
                .allergens(_allergenRepo.findAll().stream().map(_syncMapper::toSyncDictionaryResponse).toList())
                .dishCategories(_dishRepo.findAllCategories().stream().map(_syncMapper::toSyncDictionaryResponse).toList())
                .banStatuses(_banRepo.findAllStatuses().stream().map(_syncMapper::toSyncDictionaryResponse).toList())
                .reportStatuses(_reportRepo.findAllStatuses().stream().map(_syncMapper::toSyncDictionaryResponse).toList())
                .orderStatuses(_orderRepo.findAllStatuses().stream().map(_syncMapper::toSyncDictionaryResponse).toList())
                .orderItemStatuses(_orderRepo.findAllItemStatuses().stream().map(_syncMapper::toSyncDictionaryResponse).toList())
                .reservationStatuses(_reservationRepo.findAllStatuses().stream().map(_syncMapper::toSyncDictionaryResponse).toList())
                .tableStatuses(_tableRepo.findAllStatuses().stream().map(_syncMapper::toSyncDictionaryResponse).toList())
                .build();
    }


    @Override
    @Auditable(action = "GET_ROLES")
    public List<SyncRoleResponse> getRoles() {
        return _roleRepo.findAll().stream().map(
                r -> SyncRoleResponse.builder()
                        .token(r.getToken())
                        .name(r.getName())
                        .build()
        ).toList();
    }

    @Override
    @Auditable(action = "SYNC_DISHES")
    public PagedResult<SyncDishResponse> getDishesSync(int page) {
        Page<Dishes> dishesPage = _dishRepo.findAll(calculatePageable(page));

        Page<SyncDishResponse> response = dishesPage.map(_syncMapper::toSyncDishResponse);

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_BANS")
    public PagedResult<SyncBanResponse> getBansSync(int page) {
        Page<Bans> bansPage = _banRepo.findAll(calculatePageable(page));

        Page<SyncBanResponse> response = bansPage.map(_syncMapper::toBanSyncResponse);

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_REPORTS")
    public PagedResult<SyncReportResponse> getReportsSync(int page) {
        Page<GuestReports> reportsPage = _reportRepo.findAll(null, calculatePageable(page));

        Page<SyncReportResponse> response = reportsPage.map(_syncMapper::toSyncReportResponse);

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_INGREDIENTS")
    public PagedResult<SyncIngredientResponse> getIngredientsSync(int page) {
        Page<Ingredients> ingredientsPage = _ingredientsRepo.findAll(calculatePageable(page));

        Page<SyncIngredientResponse> response = ingredientsPage.map(_syncMapper::toSyncIngredientResponse);

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_ORDERS")
    public PagedResult<SyncOrderResponse> getOrdersSync(int page) {
        Page<Orders> ordersPage = _orderRepo.findAll(calculatePageable(page));

        Page<SyncOrderResponse> response = ordersPage.map(_syncMapper::toSyncOrderResponse);

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_ORDER_ITEMS")
    public PagedResult<SyncOrderItemResponse> getOrderItemsSync(int page) {
        Page<OrderItems> itemsPage = _orderRepo.findAllItems(calculatePageable(page));

        Page<SyncOrderItemResponse> response = itemsPage.map(_syncMapper::toSyncOrderItemResponse);

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_RESERVATIONS")
    public PagedResult<SyncReservationResponse> getReservationsSync(int page) {
        Page<Reservations> reservationsPage = _reservationRepo.findAll(null, calculatePageable(page));

        Page<SyncReservationResponse> response = reservationsPage.map(_syncMapper::toSyncReservationResponse);

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_TABLES")
    public PagedResult<SyncTableResponse> getTablesSync(int page) {
        Page<RestaurantTables> tablesPage = _tableRepo.findAll(calculatePageable(page));

        Page<SyncTableResponse> response = tablesPage.map(_syncMapper::toSyncTableResponse);

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_USERS")
    public PagedResult<SyncUserResponse> getUsersSync(int page) {
        Page<Users> usersPage = _userRepo.findAllUsers(null, calculatePageable(page));

        Page<SyncUserResponse> response = usersPage.map(_syncMapper::toSyncUserResponse);

        return new PagedResult<>(response);
    }

    private void addModuleMetadata(
            Map<String, SyncBootstrapResponse.EntityMetadata> modules,
            String key,
            long count
    ) {
        modules.put(key, new SyncBootstrapResponse.EntityMetadata(
                count,
                calculatePage(count)
        ));
    }

    private Pageable calculatePageable(int page) {
        int pageIndex = Math.max(0, page - 1);
        return PageRequest.of(pageIndex, DEFAULT_PAGE_SIZE);
    }

    private int calculatePage(long count) {
        return (int) Math.ceil((double) count / DEFAULT_PAGE_SIZE);
    }
}
