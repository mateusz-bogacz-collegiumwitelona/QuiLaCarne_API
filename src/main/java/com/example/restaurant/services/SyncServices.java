package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.response.*;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.models.*;
import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.base.BaseNamedEntity;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.repository.interfaces.*;
import com.example.restaurant.services.interfaces.ISyncServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    private final int DEFAULT_PAGE_SIZE = 20;

    @Value("${application.storage.s3.public-endpoint}")
    private String s3Endpoint;

    @Value("${application.storage.s3.bucket-name}")
    private String s3BucketName;

    @Override
    @Auditable(action = "BOOTSTRAP_MANIFEST")
    public SyncBootstrapResponse getBootstrapManifest() {
        Map<String, SyncBootstrapResponse.EntityMetadata> modules = new HashMap<>();

        addModuleMetadata(modules, "roles", _roleRepo.count());
        addModuleMetadata(modules, "allergens", _allergenRepo.count());
        addModuleMetadata(modules, "ingredients", _ingredientsRepo.count());
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
                .allergens(mapToSync(_allergenRepo.findAll()))
                .dishCategories(mapToSync(_dishRepo.findAllCategories()))
                .banStatuses(mapToSync(_banRepo.findAllStatuses()))
                .reportStatuses(mapToSync(_reportRepo.findAllStatuses()))
                .orderStatuses(mapToSync(_orderRepo.findAllStatuses()))
                .orderItemStatuses(mapToSync(_orderRepo.findAllItemStatuses()))
                .reservationStatuses(mapToSync(_reservationRepo.findAllStatuses()))
                .tableStatuses(mapToSync(_tableRepo.findAllStatuses()))
                .build();
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

        Page<SyncDishResponse> response = dishesPage.map(d -> {
            String categoryToken = d.getCategory() != null ? d.getCategory().getToken() : null;

            List<String> ingredientTokens = d.getIngredients() != null
                    ? d.getIngredients().stream()
                      .map(BaseEntity::getToken)
                      .toList()
                    : List.of();

            String imageUrl = d.getImageUrl();

            if (imageUrl != null && !imageUrl.startsWith("http")) {
                if (s3Endpoint != null && !s3Endpoint.isBlank() && s3BucketName != null) {
                    imageUrl = String.format("%s/%s/%s", s3Endpoint.trim(), s3BucketName, imageUrl);
                }
            }

            return SyncDishResponse.builder()
                    .token(d.getToken())
                    .name(d.getName())
                    .price(d.getPrice())
                    .isAvailable(d.isAvailable())
                    .unavailableReason(d.getUnavailableReason())
                    .imageUrl(imageUrl)
                    .categoryToken(categoryToken)
                    .ingredientTokens(ingredientTokens)
                    .build();
        });

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_BANS")
    public PagedResult<SyncBanResponse> getBansSync(int page) {
        Page<Bans> bansPage = _banRepo.findAll(calculatePageable(page));

        Page<SyncBanResponse> response = bansPage.map(b -> {
            String userToken = b.getUser() != null ? b.getUser().getToken() : null;
            String bannedByToken = b.getBannedBy() != null ? b.getBannedBy().getToken() : null;

            List<String> statusToken = b.getBanStatuses() != null
                    ? b.getBanStatuses().stream()
                      .map(BaseEntity::getToken)
                      .toList()
                    : List.of();

            return SyncBanResponse.builder()
                    .token(b.getToken())
                    .userToken(userToken)
                    .bannedByToken(bannedByToken)
                    .statusTokens(statusToken)
                    .reason(b.getReason())
                    .expiresAt(b.getExpiresAt())
                    .isActive(b.getIsActive())
                    .createdAt(b.getCreatedAt())
                    .updatedAt(b.getUpdatedAt())
                    .build();
        });

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_REPORTS")
    public PagedResult<SyncReportResponse> getReportsSync(int page) {
        Page<GuestReports> reportsPage = _reportRepo.findAll(null, calculatePageable(page));

        Page<SyncReportResponse> response = reportsPage.map(r -> {
            String guestToken = r.getGuest() != null ? r.getGuest().getToken() : null;
            String reporterToken = r.getReporter() != null ? r.getReporter().getToken() : null;

            List<String> statusTokens = r.getStatuses() != null
                    ? r.getStatuses().stream()
                      .map(BaseEntity::getToken)
                      .toList()
                    : List.of();

            return SyncReportResponse.builder()
                    .token(r.getToken())
                    .guestToken(guestToken)
                    .reporterToken(reporterToken)
                    .statusTokens(statusTokens)
                    .reason(r.getReason())
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .build();
        });

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_INGREDIENTS")
    public PagedResult<SyncIngredientResponse> getIngredientsSync(int page) {
        Page<Ingredients> ingredientsPage = _ingredientsRepo.findAll(calculatePageable(page));

        Page<SyncIngredientResponse> response = ingredientsPage.map(i -> {
            List<String> allergenTokens = i.getAllergens() != null
                    ? i.getAllergens().stream()
                      .map(BaseEntity::getToken)
                      .toList()
                    : List.of();

            return SyncIngredientResponse.builder()
                    .token(i.getToken())
                    .nameEn(i.getNameEn())
                    .namePl(i.getNamePl())
                    .allergenTokens(allergenTokens)
                    .build();
        });

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_ORDERS")
    public PagedResult<SyncOrderResponse> getOrdersSync(int page) {
        Page<Orders> ordersPage = _orderRepo.findAll(calculatePageable(page));

        Page<SyncOrderResponse> response = ordersPage.map(o -> {
            String reservationToken = o.getReservation() != null ? o.getReservation().getToken() : null;
            String tableToken = o.getTable() != null ? o.getTable().getToken() : null;
            String waiterToken = o.getWaiter() != null ? o.getWaiter().getToken() : null;

            List<String> statusTokens = o.getStatuses() != null
                    ? o.getStatuses().stream().map(BaseEntity::getToken).toList()
                    : List.of();

            return SyncOrderResponse.builder()
                    .token(o.getToken())
                    .reservationToken(reservationToken)
                    .tableToken(tableToken)
                    .waiterToken(waiterToken)
                    .totalPrice(o.getTotalPrice())
                    .statusTokens(statusTokens)
                    .createdAt(o.getCreatedAt())
                    .updatedAt(o.getUpdatedAt())
                    .build();
        });

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_ORDER_ITEMS")
    public PagedResult<SyncOrderItemResponse> getOrderItemsSync(int page) {
        Page<OrderItems> itemsPage = _orderRepo.findAllItems(calculatePageable(page));

        Page<SyncOrderItemResponse> response = itemsPage.map(i -> {
            String orderToken = i.getOrder() != null ? i.getOrder().getToken() : null;
            String productToken = i.getProduct() != null ? i.getProduct().getToken() : null;

            List<String> statusTokens = i.getStatuses() != null
                    ? i.getStatuses().stream().map(BaseEntity::getToken).toList()
                    : List.of();

            return SyncOrderItemResponse.builder()
                    .token(i.getToken())
                    .orderToken(orderToken)
                    .productToken(productToken)
                    .quantity(i.getQuantity())
                    .priceAtTimeOfOrder(i.getPriceAtTimeOfOrder())
                    .note(i.getNote())
                    .statusTokens(statusTokens)
                    .createdAt(i.getCreatedAt())
                    .updatedAt(i.getUpdatedAt())
                    .build();
        });

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_RESERVATIONS")
    public PagedResult<SyncReservationResponse> getReservationsSync(int page) {
        Page<Reservations> reservationsPage = _reservationRepo.findAll(null, calculatePageable(page));

        Page<SyncReservationResponse> response = reservationsPage.map(r -> {
            String userToken = r.getUser() != null ? r.getUser().getToken() : null;
            String tableToken = r.getTableId() != null ? r.getTableId().getToken() : null;

            List<String> statusTokens = r.getReservationStatus() != null
                    ? r.getReservationStatus().stream().map(BaseEntity::getToken).toList()
                    : List.of();

            return SyncReservationResponse.builder()
                    .token(r.getToken())
                    .userToken(userToken)
                    .tableToken(tableToken)
                    .statusTokens(statusTokens)
                    .startTime(r.getStartTime())
                    .endTime(r.getEndTime())
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .build();
        });

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_TABLES")
    public PagedResult<SyncTableResponse> getTablesSync(int page) {
        Page<RestaurantTables> tablesPage = _tableRepo.findAll(calculatePageable(page));

        Page<SyncTableResponse> response = tablesPage.map(t -> {
            List<String> statusTokens = t.getTableStatus() != null
                    ? t.getTableStatus().stream().map(BaseEntity::getToken).toList()
                    : List.of();

            return SyncTableResponse.builder()
                    .token(t.getToken())
                    .tableNumber(t.getTableNumber())
                    .capacity(t.getCapacity())
                    .statusTokens(statusTokens)
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
                    .build();
        });

        return new PagedResult<>(response);
    }

    @Override
    @Auditable(action = "SYNC_USERS")
    public PagedResult<SyncUserResponse> getUsersSync(int page) {
        Page<Users> usersPage = _userRepo.findAllUsers(null, calculatePageable(page));

        Page<SyncUserResponse> response = usersPage.map(u -> {
            List<String> roleTokens = u.getRoles() != null
                    ? u.getRoles().stream().map(BaseEntity::getToken).toList()
                    : List.of();

            boolean isStaff = u.getRoles() != null && u.getRoles().stream()
                    .anyMatch(r -> r.getName().equals("ROLE_WAITER")
                            || r.getName().equals("ROLE_MANAGER")
                            || r.getName().equals("ROLE_ADMIN"));

            return SyncUserResponse.builder()
                    .token(u.getToken())
                    .username(u.getUsername())
                    .email(u.getEmail())
                    .isActive(u.getIsActive())
                    .isStaff(isStaff)
                    .roleTokens(roleTokens)
                    .createdAt(u.getCreatedAt())
                    .updatedAt(u.getUpdatedAt())
                    .build();
        });

        return new PagedResult<>(response);
    }

    private Pageable calculatePageable(int page) {
        int pageIndex = Math.max(0, page - 1);
        return PageRequest.of(pageIndex, DEFAULT_PAGE_SIZE);
    }

    private int calculatePage(long count) {
        return (int) Math.ceil((double) count / DEFAULT_PAGE_SIZE);
    }

    private <T extends BaseEntity> List<SyncDictionaryResponse> mapToSync(List<T> entities) {
        return entities.stream().map(entity -> {
            if (entity instanceof BaseTranslatedEntity translated) {
                return new SyncDictionaryResponse(
                        translated.getToken(),
                        translated.getNameEn(),
                        translated.getNamePl()
                );
            } else if (entity instanceof BaseNamedEntity named) {
                return new SyncDictionaryResponse(
                        named.getToken(),
                        named.getName(),
                        named.getName()
                );
            } else {
                throw new IllegalArgumentException("Entity type not supported for dictionary mapping: "
                        + entity.getClass().getSimpleName());
            }
        }).toList();
    }
}
