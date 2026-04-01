package com.example.restaurant.mappers;

import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DictionaryMapperTest {
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
        List<EntityResponse> result = DictionaryMapper.map(null, "pl");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("map: Should return empty list when entities list is empty")
    void map_ShouldReturnEmptyList_WhenEntitiesAreEmpty() {
        List<EntityResponse> result = DictionaryMapper.map(new ArrayList<>(), "en");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("map: Should map to Polish names when lang is 'pl'")
    void map_ShouldMapToPolishNames_WhenLangIsPl() {
        // Arrange
        List<DummyEntity> entities = List.of(
                new DummyEntity("TOKEN_1", "Jabłko", "Apple"),
                new DummyEntity("TOKEN_2", "Gruszka", "Pear")
        );

        List<EntityResponse> result = DictionaryMapper.map(entities, "pl");

        assertEquals(2, result.size());
        assertEquals("TOKEN_1", result.get(0).getToken());
        assertEquals("Jabłko", result.get(0).getName());
        assertEquals("TOKEN_2", result.get(1).getToken());
        assertEquals("Gruszka", result.get(1).getName());
    }

    @Test
    @DisplayName("map: Should map to English names when lang is not 'pl'")
    void map_ShouldMapToEnglishNames_WhenLangIsNotPl() {
        // Arrange
        List<DummyEntity> entities = List.of(
                new DummyEntity("TOKEN_1", "Jabłko", "Apple"),
                new DummyEntity("TOKEN_2", "Gruszka", "Pear")
        );

        List<EntityResponse> result = DictionaryMapper.map(entities, "en");

        assertEquals(2, result.size());
        assertEquals("Apple", result.get(0).getName());
        assertEquals("Pear", result.get(1).getName());
    }

    @Test
    @DisplayName("map: Should ignore case for lang parameter")
    void map_ShouldIgnoreCase_WhenLangIsPlWithDifferentCases() {
        List<DummyEntity> entities = List.of(new DummyEntity("T1", "Polski", "English"));

        assertEquals("Polski", DictionaryMapper.map(entities, "PL").get(0).getName());
        assertEquals("Polski", DictionaryMapper.map(entities, "Pl").get(0).getName());
        assertEquals("Polski", DictionaryMapper.map(entities, "pl").get(0).getName());
    }
}