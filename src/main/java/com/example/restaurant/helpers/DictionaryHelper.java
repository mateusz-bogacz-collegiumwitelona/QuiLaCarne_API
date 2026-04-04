package com.example.restaurant.helpers;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DictionaryHelper {
    public static <T extends BaseTranslatedEntity> List<EntityResponse> map(List<T> entities, String lang) {
        if (ObjectUtils.isEmpty(entities)) return List.of();

        return entities.stream().map(e -> EntityResponse.builder()
                .token(e.getToken())
                .name("pl".equalsIgnoreCase(lang) ? e.getNamePl() : e.getNameEn())
                .build()
        ).toList();
    }

    public static <T extends BaseTranslatedEntity> T createEntity(
            Supplier<T> supplier,
            AddEntityRequest request,
            BiPredicate<String, String> isNameTaken,
            String errorMessage
    ) {
        String namePl = request.getNamePl().trim();
        String nameEn = request.getNameEn().trim();

        if (isNameTaken.test(namePl, nameEn)) throw new EntityAlreadyExistsException(errorMessage);

        T entity = supplier.get();

        entity.setNamePl(namePl);
        entity.setNameEn(nameEn);
        entity.setToken(nameEn.toUpperCase().replace(" ", "_"));

        return entity;
    }

    public static <T extends BaseTranslatedEntity> void deleteEntity(
            String token,
            Function<String, T> findByToken,
            Consumer<T> saveEntity,
            Consumer<T> relatedEntityCleanup
    ) {
        T entity = findByToken.apply(token);

        if (relatedEntityCleanup != null) relatedEntityCleanup.accept(entity);

        entity.setToken(SoftDeleteHelpers.markAsDelete(entity.getToken()));
        entity.setNameEn(SoftDeleteHelpers.markAsDelete(entity.getNameEn()));
        entity.setNamePl(SoftDeleteHelpers.markAsDelete(entity.getNamePl()));
        entity.setDeletedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));

        saveEntity.accept(entity);
    }
}
