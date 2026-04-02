package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.jpa.IJpaIngredientsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngredientsRepositoryTest {

    @Mock
    private IJpaIngredientsRepository _jpaIngredientsRepo;

    @InjectMocks
    private IngredientsRepository _ingredientsRepository;

    @Test
    @DisplayName("Save: Success")
    void save_ShouldCallJpaSave() {
        Ingredients ingredient = new Ingredients();
        _ingredientsRepository.save(ingredient);

        verify(_jpaIngredientsRepo, times(1)).save(ingredient);
    }

    @Test
    @DisplayName("Is name taken: return true if polish name is taken")
    void isNameTaken_ShouldReturnTrue_WhenPlNameExists() {
        when(_jpaIngredientsRepo.findByNamePl(TestConstants.INGREDIENT_PL)).thenReturn(Optional.of(new Ingredients()));

        boolean result = _ingredientsRepository.isNameTaken(TestConstants.INGREDIENT_PL, TestConstants.INGREDIENT_EN);

        assertTrue(result);
        verify(_jpaIngredientsRepo, times(1)).findByNamePl(TestConstants.INGREDIENT_PL);
    }

    @Test
    @DisplayName("Is name taken: return true if english name is taken")
    void isNameTaken_ShouldReturnTrue_WhenEnNameExists() {
        when(_jpaIngredientsRepo.findByNamePl(anyString())).thenReturn(Optional.empty());
        when(_jpaIngredientsRepo.findByNameEn(TestConstants.INGREDIENT_EN)).thenReturn(Optional.of(new Ingredients()));

        boolean result = _ingredientsRepository.isNameTaken(TestConstants.INGREDIENT_PL, TestConstants.INGREDIENT_EN);

        assertTrue(result);
        verify(_jpaIngredientsRepo, times(1)).findByNamePl(TestConstants.INGREDIENT_PL);
        verify(_jpaIngredientsRepo, times(1)).findByNameEn(TestConstants.INGREDIENT_EN);
    }

    @Test
    @DisplayName("Is name taken: return false if polish and english name isn't taken")
    void isNameTaken_ShouldReturnFalse_WhenNamesAreFree() {
        when(_jpaIngredientsRepo.findByNamePl("Cebula")).thenReturn(Optional.empty());
        when(_jpaIngredientsRepo.findByNameEn("Onion")).thenReturn(Optional.empty());

        boolean result = _ingredientsRepository.isNameTaken("Cebula", "Onion");

        assertFalse(result);
    }

    @Test
    @DisplayName("Is name taken: should not checking english name if polish name is taken")
    void isNameTaken_ShouldNotCheckEnglishName_IfPolishNameIsAlreadyTaken() {
        when(_jpaIngredientsRepo.findByNamePl("Cebula")).thenReturn(Optional.of(new Ingredients()));

        _ingredientsRepository.isNameTaken("Cebula", "Onion");

        verify(_jpaIngredientsRepo, times(1)).findByNamePl("Cebula");
        verify(_jpaIngredientsRepo, never()).findByNameEn(anyString());
    }

    @Test
    @DisplayName("Find by token: return ingrediant if exist")
    void findByToken_ShouldReturnIngredient_WhenExists() {
        Ingredients ingredient = new Ingredients();
        when(_jpaIngredientsRepo.findByToken(TestConstants.TOKEN_TOMATO)).thenReturn(Optional.of(ingredient));

        Ingredients result = _ingredientsRepository.findByToken(TestConstants.TOKEN_TOMATO);

        assertNotNull(result);
        assertEquals(ingredient, result);
    }

    @Test
    @DisplayName("Find by token: throw exception if ingridients not found")
    void findByToken_ShouldThrowException_WhenNotFound() {
        when(_jpaIngredientsRepo.findByToken(TestConstants.TOKEN_NON_EXISTENT)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> _ingredientsRepository.findByToken(
                TestConstants.TOKEN_NON_EXISTENT
        ));
        assertEquals("Ingredient not found", exception.getMessage());
    }

    @Test
    @DisplayName("findAll: Should return list of ingredients from JPA")
    void findAll_ShouldReturnListOfIngredients() {
        List<Ingredients> expectedIngredients = List.of(new Ingredients(), new Ingredients());

        when(_jpaIngredientsRepo.findAll()).thenReturn(expectedIngredients);

        List<Ingredients> result = _ingredientsRepository.findAll();

        assertEquals(expectedIngredients, result);
        verify(_jpaIngredientsRepo, times(1)).findAll();
    }
}