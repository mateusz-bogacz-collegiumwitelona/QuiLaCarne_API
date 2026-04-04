package com.example.restaurant.helpers;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DictionaryHelperTest {
    private static class DummyEntity extends BaseTranslatedEntity {
        public DummyEntity(String token, String namePl, String nameEn) {
            this.setToken(token);
            this.setNamePl(namePl);
            this.setNameEn(nameEn);
        }
    }

    @Test
    @DisplayName("map: Should return empty list when entities list is null")
    void map_ShouldReturnEmptyList_WhenEntitiesAreNull() {
        List<EntityResponse> result = DictionaryHelper.map(null, "pl");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("map: Should return empty list when entities list is empty")
    void map_ShouldReturnEmptyList_WhenEntitiesAreEmpty() {
        List<EntityResponse> result = DictionaryHelper.map(new ArrayList<>(), "en");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("map: Should map to Polish names when lang is 'pl'")
    void map_ShouldMapToPolishNames_WhenLangIsPl() {
        List<DummyEntity> entities = List.of(
                new DummyEntity(TestConstants.TOKEN_1, "Jabłko", "Apple"),
                new DummyEntity(TestConstants.TOKEN_2, "Gruszka", "Pear")
        );

        List<EntityResponse> result = DictionaryHelper.map(entities, TestConstants.LANG_PL);

        assertEquals(2, result.size());
        assertEquals(TestConstants.TOKEN_1, result.get(0).getToken());
        assertEquals("Jabłko", result.get(0).getName());
        assertEquals(TestConstants.TOKEN_2, result.get(1).getToken());
        assertEquals("Gruszka", result.get(1).getName());
    }

    @Test
    @DisplayName("map: Should map to English names when lang is not 'pl'")
    void map_ShouldMapToEnglishNames_WhenLangIsNotPl() {
        List<DummyEntity> entities = List.of(
                new DummyEntity(TestConstants.TOKEN_1, "Jabłko", "Apple"),
                new DummyEntity(TestConstants.TOKEN_2, "Gruszka", "Pear")
        );

        List<EntityResponse> result = DictionaryHelper.map(entities, TestConstants.LANG_EN);

        assertEquals(2, result.size());
        assertEquals("Apple", result.get(0).getName());
        assertEquals("Pear", result.get(1).getName());
    }

    @Test
    @DisplayName("map: Should ignore case for lang parameter")
    void map_ShouldIgnoreCase_WhenLangIsPlWithDifferentCases() {
        List<DummyEntity> entities = List.of(new DummyEntity("T1", "Polski", "English"));

        assertEquals("Polski", DictionaryHelper.map(entities, "PL").getFirst().getName());
        assertEquals("Polski", DictionaryHelper.map(entities, "Pl").getFirst().getName());
        assertEquals("Polski", DictionaryHelper.map(entities, "pl").getFirst().getName());
    }
}