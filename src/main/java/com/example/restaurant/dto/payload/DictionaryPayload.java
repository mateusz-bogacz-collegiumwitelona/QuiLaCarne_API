package com.example.restaurant.dto.payload;

import com.example.restaurant.models.base.BaseTranslatedEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryPayload {
    private String token;
    private String namePl;
    private String nameEn;

    public static DictionaryPayload fromEntity(BaseTranslatedEntity entity) {
        return new DictionaryPayload(
                entity.getToken(),
                entity.getNamePl(),
                entity.getNameEn()
        );
    }
}
