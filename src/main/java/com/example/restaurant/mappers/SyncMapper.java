package com.example.restaurant.mappers;

import com.example.restaurant.dto.response.SyncDictionaryResponse;
import com.example.restaurant.dto.response.SyncDishResponse;
import com.example.restaurant.dto.response.SyncUserResponse;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.base.BaseNamedEntity;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.models.lookup.Roles;
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
                .anyMatch(r -> r.getName().equals("ROLE_WAITER")
                        || r.getName().equals("ROLE_MANAGER")
                        || r.getName().equals("ROLE_ADMIN"));
    }

    public SyncDictionaryResponse toSyncDictionaryResponse(BaseTranslatedEntity entity) {
        if (entity == null) return null;
        return new SyncDictionaryResponse(
                entity.getToken(),
                entity.getNameEn(),
                entity.getNamePl()
        );
    }

    public SyncDictionaryResponse toSyncDictionaryResponse(BaseNamedEntity entity) {
        if (entity == null) return null;
        return new SyncDictionaryResponse(
                entity.getToken(),
                entity.getName(),
                entity.getName()
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
        if (imageUrl != null && !imageUrl.startsWith("http")) {
            if (s3Endpoint != null && !s3Endpoint.isBlank() && s3BucketName != null) {
                return String.format("%s/%s/%s", s3Endpoint.trim(), s3BucketName, imageUrl);
            }
        }
        return imageUrl;
    }
}