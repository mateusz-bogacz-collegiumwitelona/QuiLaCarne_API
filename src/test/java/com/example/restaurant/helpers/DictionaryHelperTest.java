package com.example.restaurant.helpers;

import static org.junit.jupiter.api.Assertions.*;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DictionaryHelperTest {
  private static class DummyEntity extends BaseTranslatedEntity {
    public DummyEntity() {}

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
    List<DummyEntity> entities =
        List.of(
            new DummyEntity(TestConstants.TOKEN_1, "Jabłko", "Apple"),
            new DummyEntity(TestConstants.TOKEN_2, "Gruszka", "Pear"));

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
    List<DummyEntity> entities =
        List.of(
            new DummyEntity(TestConstants.TOKEN_1, "Jabłko", "Apple"),
            new DummyEntity(TestConstants.TOKEN_2, "Gruszka", "Pear"));

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

  @Test
  @DisplayName("createEntity: Should return mapped entity with trimmed names and correct token")
  void createEntity_ShouldReturnMappedEntity() {
    AddEntityRequest request = new AddEntityRequest();
    request.setNameEn("Peanuts And Nuts  ");
    request.setNamePl(" Orzechy         ");

    DummyEntity result =
        DictionaryHelper.createEntity(DummyEntity::new, request, (pl, en) -> false, "Error");

    assertNotNull(result);
    assertEquals("Orzechy", result.getNamePl());
    assertEquals("Peanuts And Nuts", result.getNameEn());
    assertEquals("PEANUTS_AND_NUTS", result.getToken());
  }

  @Test
  @DisplayName("createEntity: Should throw EntityAlreadyExistsException when name is taken")
  void createEntity_ShouldThrowException_WhenNameTaken() {
    AddEntityRequest request = new AddEntityRequest();
    request.setNamePl("Orzechy");
    request.setNameEn("Peanuts");

    EntityAlreadyExistsException exception =
        assertThrows(
            EntityAlreadyExistsException.class,
            () ->
                DictionaryHelper.createEntity(
                    DummyEntity::new, request, (pl, en) -> true, "Entity already exists"));

    assertEquals("Entity already exists", exception.getMessage());
  }

  @Test
  @DisplayName("deleteEntity: Should execute cleanup, apply soft delete and save entity")
  void deleteEntity_ShouldExecuteCleanupAndSoftDelete() {
    String originalToken = "SOME_TOKEN";
    String originalPl = "Nazwa PL";
    String originalEn = "Name EN";

    DummyEntity entity = new DummyEntity(originalToken, originalPl, originalEn);

    boolean[] cleanupCalled = {false};
    boolean[] saveCalled = {false};

    DictionaryHelper.deleteEntity(
        originalToken,
        token -> entity,
        savedEntity -> {
          saveCalled[0] = true;
          assertNotNull(savedEntity.getDeletedAt());
          assertNotEquals(originalToken, savedEntity.getToken());
          assertNotEquals(originalPl, savedEntity.getNamePl());
          assertNotEquals(originalEn, savedEntity.getNameEn());
        },
        e -> {
          cleanupCalled[0] = true;
          assertEquals(originalToken, e.getToken());
          assertNull(e.getDeletedAt());
        });

    assertTrue(cleanupCalled[0], "Cleanup block should be executed");
    assertTrue(saveCalled[0], "Save block should be executed");
  }

  @Test
  @DisplayName("deleteEntity: Should execute soft delete when cleanup block is null")
  void deleteEntity_ShouldSoftDelete_WhenCleanupIsNull() {
    DummyEntity entity = new DummyEntity("TOKEN", "PL", "EN");
    boolean[] saveCalled = {false};

    assertDoesNotThrow(
        () ->
            DictionaryHelper.deleteEntity(
                "TOKEN", token -> entity, savedEntity -> saveCalled[0] = true, null));

    assertTrue(saveCalled[0], "Save block should be executed");
    assertNotNull(entity.getDeletedAt());
  }
}
