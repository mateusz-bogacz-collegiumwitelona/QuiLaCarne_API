package com.example.restaurant.mappers;

import static org.junit.jupiter.api.Assertions.*;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.dto.response.IngredientListResponse;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.models.lookup.Allergens;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DishMapperTest {

  private DishMapper _dishMapper;

  @BeforeEach
  void setUp() {
    _dishMapper =
        new DishMapper() {
          @Override
          public DishListResponse toDishListResponse(Dishes dish, String lang) {
            return null;
          }

          @Override
          public IngredientListResponse toIngredientListResponse(
              Ingredients ingredients, String lang) {
            return null;
          }
        };
  }

  @Test
  @DisplayName("translateByLang: Should return null when entity is null")
  void translateByLang_ShouldReturnNull_WhenEntityIsNull() {
    assertNull(_dishMapper.translateByLang(null, TestConstants.LANG_PL));
  }

  @Test
  @DisplayName("translateByLang: Should return correct translation based on language")
  void translateByLang_ShouldReturnTranslatedString() {
    DummyTranslatedEntity entity = new DummyTranslatedEntity("Jabłko", "Apple");

    assertEquals("Jabłko", _dishMapper.translateByLang(entity, TestConstants.LANG_PL));
    assertEquals("Apple", _dishMapper.translateByLang(entity, TestConstants.LANG_EN));
  }

  @Test
  @DisplayName("mapAllergenList: Should return empty list when allergens set is null")
  void mapAllergenList_ShouldReturnEmptyList_WhenAllergensAreNull() {
    List<String> result = _dishMapper.mapAllergenList(null, TestConstants.LANG_PL);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("mapAllergenList: Should map and translate a set of allergens")
  void mapAllergenList_ShouldReturnTranslatedList() {
    Allergens a1 = new Allergens();
    a1.setNamePl("Mleko");
    a1.setNameEn("Milk");

    Allergens a2 = new Allergens();
    a2.setNamePl("Orzechy");
    a2.setNameEn("Nuts");

    List<String> resultPl = _dishMapper.mapAllergenList(Set.of(a1, a2), TestConstants.LANG_PL);
    List<String> resultEn = _dishMapper.mapAllergenList(Set.of(a1, a2), "en");

    assertEquals(2, resultPl.size());
    assertTrue(resultPl.contains("Mleko"));
    assertTrue(resultPl.contains("Orzechy"));

    assertEquals(2, resultEn.size());
    assertTrue(resultEn.contains("Milk"));
    assertTrue(resultEn.contains("Nuts"));
  }

  private static class DummyTranslatedEntity extends BaseTranslatedEntity {
    public DummyTranslatedEntity(String pl, String en) {
      this.setNamePl(pl);
      this.setNameEn(en);
    }
  }
}
