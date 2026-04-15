package com.example.restaurant.mappers;

import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.dto.response.IngredientListResponse;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.models.lookup.Allergens;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface DishMapper {

    @Mapping(target = "name", source = "name")
    @Mapping(target = "categoryName", source = "category", qualifiedByName = "translateByLang")
    @Mapping(target = "isActive", source = "available")
    DishListResponse toDishListResponse(Dishes dish, @Context String lang);

    @Mapping(target = "name", source = "ingredients", qualifiedByName = "translateByLang")
    @Mapping(target = "allergens", source = "allergens", qualifiedByName = "mapAllergenList")
    IngredientListResponse toIngredientListResponse(Ingredients ingredients, @Context String lang);

    @Named("translateByLang")
    default String translateByLang(BaseTranslatedEntity entity, @Context String lang) {
        if (entity == null) return null;
        return entity.translate(lang);
    }

    @Named("mapAllergenList")
    default List<String> mapAllergenList(Set<Allergens> allergens, @Context String lang) {
        if (allergens == null) return new ArrayList<>();
        return allergens.stream()
                .map(a -> translateByLang(a, lang))
                .toList();
    }
}