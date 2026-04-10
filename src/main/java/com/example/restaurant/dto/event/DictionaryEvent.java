package com.example.restaurant.dto.event;

import com.example.restaurant.models.base.BaseTranslatedEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryEvent {
    private String token;
    private String namePl;
    private String nameEn;

    public static DictionaryEvent fromEntity(BaseTranslatedEntity entity) {
        return new DictionaryEvent(
                entity.getToken(),
                entity.getNamePl(),
                entity.getNameEn()
        );
    }
}
