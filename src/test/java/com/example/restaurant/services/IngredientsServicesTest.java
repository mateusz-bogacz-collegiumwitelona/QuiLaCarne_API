package com.example.restaurant.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.ingridients.IngredientSyncPublisher;
import com.example.restaurant.services.ingridients.IngredientsServices;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
class IngredientsServicesTest {
  @Mock private IIngredientsRepository _ingredientsRepo;

  @Mock private IAllergensRepository _allergensRepo;

  @Mock private IDishRepository _dishRepo;

  @Mock private IngredientSyncPublisher _syncPublisher;

  @InjectMocks private IngredientsServices _ingredientsServices;

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  @DisplayName("Adding Ingredient: Should save the component with the correct token")
  void add_ShouldSaveIngredient_WhenDataIsCorrect() {
    AddEntityRequest entityReq = new AddEntityRequest();
    entityReq.setNamePl(TestConstants.INGREDIENT_PL);
    entityReq.setNameEn(TestConstants.INGREDIENT_EN);

    AddIngredientRequest request = new AddIngredientRequest();
    request.setEntity(entityReq);
    request.setAllergenTokens(Set.of(TestConstants.TOKEN_GLUTEN));

    Allergens mockAllergen = new Allergens();
    mockAllergen.setToken(TestConstants.TOKEN_GLUTEN);
    when(_ingredientsRepo.isNameTaken(anyString(), anyString())).thenReturn(false);
    when(_allergensRepo.findAllergens(anyList())).thenReturn(List.of(mockAllergen));

    assertDoesNotThrow(() -> _ingredientsServices.add(request));

    verify(_ingredientsRepo, times(1))
        .save(
            argThat(
                ingredient ->
                    ingredient.getNamePl().equals(TestConstants.INGREDIENT_PL)
                        && ingredient.getNameEn().equals(TestConstants.INGREDIENT_EN)
                        && ingredient
                            .getToken()
                            .equals(TestConstants.INGREDIENT_EN.toUpperCase())));

    verify(_syncPublisher, times(1)).publishIngredientsCreate(any(Ingredients.class));
  }

  @Test
  @DisplayName("Adding Ingredient: Throws EntityAlreadyExistsException when name is taken")
  void add_ShouldThrowException_WhenNameIsTaken() {
    AddEntityRequest entityReq = new AddEntityRequest();
    entityReq.setNamePl(TestConstants.INGREDIENT_PL);
    entityReq.setNameEn(TestConstants.INGREDIENT_EN);
    AddIngredientRequest request = new AddIngredientRequest();
    request.setEntity(entityReq);

    when(_ingredientsRepo.isNameTaken(TestConstants.INGREDIENT_PL, TestConstants.INGREDIENT_EN))
        .thenReturn(true);

    assertThrows(EntityAlreadyExistsException.class, () -> _ingredientsServices.add(request));
    verify(_ingredientsRepo, never()).save(any());
  }

  @Test
  @DisplayName("Adding Ingredient: Throws IllegalStateException when not all allergens are found")
  void add_ShouldThrowException_WhenAllergensNotFound() {
    AddEntityRequest entityReq = new AddEntityRequest();
    entityReq.setNamePl(TestConstants.INGREDIENT_PL);
    entityReq.setNameEn(TestConstants.INGREDIENT_EN);
    AddIngredientRequest request = new AddIngredientRequest();
    request.setEntity(entityReq);
    request.setAllergenTokens(Set.of(TestConstants.TOKEN_GLUTEN, TestConstants.TOKEN_1));

    when(_ingredientsRepo.isNameTaken(anyString(), anyString())).thenReturn(false);
    when(_allergensRepo.findAllergens(anyList())).thenReturn(List.of(new Allergens()));

    assertThrows(IllegalStateException.class, () -> _ingredientsServices.add(request));
  }

  @Test
  @DisplayName(
      "Removing an ingredient: Success - should anonymize data "
          + "(Soft Delete) and deactivate associated dishes")
  void remove_ShouldSoftDeleteIngredient_AndDeactivateDishes() {
    String token = TestConstants.TOKEN_INGREDIENT;
    UUID ingredientId = UUID.randomUUID();

    Ingredients ingredient = new Ingredients();
    ingredient.setId(ingredientId);
    ingredient.setToken(token);
    ingredient.setNameEn(TestConstants.INGREDIENT_EN);
    ingredient.setNamePl(TestConstants.INGREDIENT_PL);

    Dishes dish = new Dishes();
    dish.setAvailable(true);

    when(_ingredientsRepo.findByToken(token)).thenReturn(ingredient);
    when(_dishRepo.findByIngredientsId(ingredientId)).thenReturn(List.of(dish));

    assertDoesNotThrow(() -> _ingredientsServices.remove(token));

    assertTrue(ingredient.getToken().startsWith("DELETED_"));
    assertTrue(ingredient.getNameEn().startsWith("DELETED_"));
    assertNotNull(ingredient.getDeletedAt());
    verify(_ingredientsRepo).save(ingredient);

    assertFalse(dish.isAvailable());
    assertEquals(TestConstants.INGREDIENT_EN + " is deleted", dish.getUnavailableReason());
    verify(_dishRepo).save(dish);

    verify(_syncPublisher, times(1)).publishIngredientsDelete(token);
  }

  @Test
  @DisplayName("getDictionary: Returns empty list when repository returns empty")
  void getDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
    when(_ingredientsRepo.findAll()).thenReturn(new ArrayList<>());
    DictionaryResponse result = _ingredientsServices.getDictionary();
    assertTrue(result.getItem().isEmpty());
  }

  @Test
  @DisplayName("getDictionary: Returns Polish names when language is pl")
  void getDictionary_ShouldReturnPolishNames_WhenLanguageIsPl() {
    LocaleContextHolder.setLocale(Locale.of(TestConstants.LANG_PL));
    Ingredients ingredient = new Ingredients();
    ingredient.setToken(TestConstants.TOKEN_INGREDIENT);
    ingredient.setNamePl(TestConstants.INGREDIENT_PL);
    ingredient.setNameEn(TestConstants.INGREDIENT_EN);

    when(_ingredientsRepo.findAll()).thenReturn(List.of(ingredient));

    DictionaryResponse result = _ingredientsServices.getDictionary();

    assertEquals(1, result.getItem().size());
    assertEquals(TestConstants.TOKEN_INGREDIENT, result.getItem().getFirst().getToken());
    assertEquals(TestConstants.INGREDIENT_PL, result.getItem().getFirst().getName());
  }

  @Test
  @DisplayName("getDictionary: Returns English names when language is not pl")
  void getDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
    Locale.of(TestConstants.LANG_EN);
    Ingredients ingredient = new Ingredients();
    ingredient.setToken(TestConstants.TOKEN_INGREDIENT);
    ingredient.setNamePl(TestConstants.INGREDIENT_PL);
    ingredient.setNameEn(TestConstants.INGREDIENT_EN);

    when(_ingredientsRepo.findAll()).thenReturn(List.of(ingredient));

    DictionaryResponse result = _ingredientsServices.getDictionary();

    assertEquals(1, result.getItem().size());
    assertEquals(TestConstants.TOKEN_INGREDIENT, result.getItem().getFirst().getToken());
    assertEquals(TestConstants.INGREDIENT_EN, result.getItem().getFirst().getName());
  }
}
