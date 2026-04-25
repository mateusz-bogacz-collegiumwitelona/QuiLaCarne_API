package com.example.restaurant.mappers;

import com.example.restaurant.dto.sync.*;
import com.example.restaurant.models.*;
import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.models.lookup.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public abstract class SyncMapper {

    @Value("${application.storage.s3.public-endpoint}")
    protected String s3Endpoint;

    @Value("${application.storage.s3.bucket-name}")
    protected String s3BucketName;

    @Mapping(target = "roleTokens", source = "roles", qualifiedByName = "mapRoleTokens")
    @Mapping(target = "isStaff", source = "roles", qualifiedByName = "checkIfStaff")
    public abstract SyncUserResponse toSyncUserResponse(Users user);

    @Named("mapRoleTokens")
    protected List<String> mapRoleTokens(Set<Roles> roles) {
        if (roles == null) return List.of();
        return roles.stream().map(BaseEntity::getToken).toList();
    }

    @Named("checkIfStaff")
    protected boolean checkIfStaff(Set<Roles> roles) {
        if (roles == null) return false;
        return roles.stream()
                .anyMatch(r -> "ROLE_WAITER".equals(r.getName())
                        || "ROLE_MANAGER".equals(r.getName())
                        || "ROLE_ADMIN".equals(r.getName()));
    }

    public SyncDictionaryResponse toSyncDictionaryResponse(BaseTranslatedEntity entity) {
        if (entity == null) return null;
        return new SyncDictionaryResponse(
                entity.getToken(),
                entity.getNameEn(),
                entity.getNamePl()
        );
    }

    @Mapping(target = "categoryToken", source = "category", qualifiedByName = "mapCategoryToken")
    @Mapping(target = "ingredientTokens", source = "ingredients", qualifiedByName = "mapIngredientTokens")
    @Mapping(target = "imageUrl", source = "imageUrl", qualifiedByName = "mapImageUrl")
    @Mapping(target = "isAvailable", source = "available")
    public abstract SyncDishResponse toSyncDishResponse(Dishes dish);

    @Named("mapCategoryToken")
    protected String mapCategoryToken(DishesCategories category) {
        return category != null ? category.getToken() : null;
    }

    @Named("mapIngredientTokens")
    protected List<String> mapIngredientTokens(Set<Ingredients> ingredients) {
        return ingredients != null
                ? ingredients.stream().map(BaseEntity::getToken).toList()
                : List.of();
    }

    @Named("mapImageUrl")
    protected String mapImageUrl(String imageUrl) {

        if (imageUrl != null
                && !imageUrl.startsWith("http")
                && s3Endpoint != null
                && !s3Endpoint.isBlank()
                && s3BucketName != null) {

            return String.format(
                    "%s/%s/%s",
                    s3Endpoint.trim(),
                    s3BucketName,
                    imageUrl
            );
        }

        return imageUrl;
    }

    @Mapping(target = "userToken", source = "user", qualifiedByName = "mapUserToken")
    @Mapping(target = "bannedByToken", source = "bannedBy", qualifiedByName = "mapUserToken")
    @Mapping(target = "statusTokens", source = "banStatuses", qualifiedByName = "mapBanStatuses")
    public abstract SyncBanResponse toBanSyncResponse(Bans ban);

    @Named("mapUserToken")
    protected String mapUserToken(Users user) {
        return user != null ? user.getToken() : null;
    }

    @Named("mapBanStatuses")
    protected List<String> mapBanStatuses(Set<BanStatus> statuses) {
        return statuses != null
                ? statuses.stream().map(BaseEntity::getToken).toList()
                : List.of();
    }


    @Mapping(target = "guestToken", source = "guest", qualifiedByName = "mapUserToken")
    @Mapping(target = "reporterToken", source = "reporter", qualifiedByName = "mapUserToken")
    @Mapping(target = "statusTokens", source = "statuses", qualifiedByName = "mapReportStatuses")
    public abstract SyncReportResponse toSyncReportResponse(GuestReports report);

    @Named("mapReportStatuses")
    public List<String> mapReportStatuses(Set<GuestReportStatus> statuses) {
        return statuses != null
                ? statuses.stream().map(BaseEntity::getToken).toList()
                : List.of();
    }

    @Mapping(target = "allergenTokens", source = "allergens", qualifiedByName = "mapAllergens")
    public abstract SyncIngredientResponse toSyncIngredientResponse(Ingredients ingredient);

    @Named("mapAllergens")
    public List<String> mapAllergens(Set<Allergens> allergens) {
        return allergens != null
                ? allergens.stream().map(BaseEntity::getToken).toList()
                : List.of();
    }

    @Mapping(target = "waiterToken", source = "waiter", qualifiedByName = "mapUserToken")
    @Mapping(target = "reservationToken", source = "reservation", qualifiedByName = "mapReservation")
    @Mapping(target = "tableToken", source = "table", qualifiedByName = "mapTableToken")
    @Mapping(target = "statusTokens", source = "statuses", qualifiedByName = "mapOrderStatuses")
    public abstract SyncOrderResponse toSyncOrderResponse(Orders order);

    @Mapping(target = "orderToken", source = "order", qualifiedByName = "mapOrder")
    @Mapping(target = "productToken", source = "product", qualifiedByName = "mapProduct")
    @Mapping(target = "statusTokens", source = "statuses", qualifiedByName = "mapOrderItemStatuses")
    public abstract SyncOrderItemResponse toSyncOrderItemResponse(OrderItems item);

    @Named("mapTableToken")
    public String mapTableToken(RestaurantTables table) {
        return table != null ? table.getToken() : null;
    }

    @Named("mapOrderStatuses")
    public List<String> mapOrderStatuses(Set<OrderStatus> statuses) {
        return statuses != null
                ? statuses.stream().map(BaseEntity::getToken).toList()
                : List.of();
    }

    @Named("mapOrder")
    public String mapOrderToken(Orders order) {
        return order != null ? order.getToken() : null;
    }

    @Named("mapProduct")
    public String mapProductToken(Dishes dish) {
        return dish != null ? dish.getToken() : null;
    }

    @Named("mapOrderItemStatuses")
    public List<String> mapOrderItemStatuses(Set<OrderItemsStatus> statuses) {
        return statuses != null
                ? statuses.stream().map(BaseEntity::getToken).toList()
                : List.of();
    }

    @Named("mapReservation")
    public String mapReservation(Reservations reservation) {
        return reservation != null ? reservation.getToken() : null;
    }

    @Mapping(target = "userToken", source = "user", qualifiedByName = "mapUserToken")
    @Mapping(target = "tableToken", source = "tableId", qualifiedByName = "mapTableToken")
    @Mapping(target = "statusTokens", source = "reservationStatus", qualifiedByName = "mapReservationStatus")
    public abstract SyncReservationResponse toSyncReservationResponse(Reservations reservation);

    @Named("mapReservationStatus")
    public List<String> mapReservationStatus(Set<ReservationStatus> statuses) {
        return statuses != null
                ? statuses.stream().map(BaseEntity::getToken).toList()
                : List.of();
    }

    @Mapping(target = "statusTokens", source = "tableStatus", qualifiedByName = "mapTableStatus")
    public abstract SyncTableResponse toSyncTableResponse(RestaurantTables table);

    @Named("mapTableStatus")
    public List<String> mapTableStatus(Set<TableStatus> statuses) {
        return statuses != null
                ? statuses.stream().map(BaseEntity::getToken).toList()
                : List.of();
    }
}