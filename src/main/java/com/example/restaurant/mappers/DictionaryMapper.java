package com.example.restaurant.mappers;

import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import org.springframework.util.ObjectUtils;

import java.util.List;

public class DictionaryMapper {
    public static <T extends BaseTranslatedEntity> List<EntityResponse> map(List<T> entities, String lang) {
        if (ObjectUtils.isEmpty(entities)) return List.of();

        return entities.stream().map(e -> EntityResponse.builder()
                .token(e.getToken())
                .name("pl".equalsIgnoreCase(lang) ? e.getNamePl() : e.getNameEn())
                .build()
        ).toList();
    }
}
