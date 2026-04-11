package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.SyncRoleResponse;
import com.example.restaurant.dto.response.SyncBootstrapResponse;
import com.example.restaurant.dto.response.SyncDictionariesResponse;
import com.example.restaurant.dto.response.SyncDictionaryResponse;
import com.example.restaurant.dto.response.SyncDishResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.models.Dishes;
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
                .ingredients(mapToSync(_ingredientsRepo.findAll()))
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
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, DEFAULT_PAGE_SIZE);

        Page<Dishes> dishesPage = _dishRepo.findAll(pageable);

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
