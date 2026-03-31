package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngredientsServicesTest {
    @Mock
    private IIngredientsRepository _ingredientsRepo;

    @Mock
    private IAllergensRepository _allergensRepo;

    @Mock
    private IDishRepository _dishRepo;

    @InjectMocks
    private IngredientsServices _ingredientsServices;

    @Test
    @DisplayName("Adding Ingredient: Should save the component with the correct token")
    void add_ShouldSaveIngredient_WhenDataIsCorrect() {
        AddEntityRequest entityReq = new AddEntityRequest();
        entityReq.setNamePl("Cebula");
        entityReq.setNameEn("Onion Ring");

        AddIngredientRequest request = new AddIngredientRequest();
        request.setEntity(entityReq);
        request.setAllergenTokens(Set.of("GLUTEN"));

        Allergens mockAllergen = new Allergens();
        mockAllergen.setToken("GLUTEN");
        when(_ingredientsRepo.isNameTaken(anyString(), anyString())).thenReturn(false);
        when(_allergensRepo.findAllergens(anyList())).thenReturn(List.of(mockAllergen));

        assertDoesNotThrow(() -> _ingredientsServices.add(request));

        verify(_ingredientsRepo, times(1)).save(argThat(ingredient ->
                ingredient.getNamePl().equals("Cebula") &&
                        ingredient.getNameEn().equals("Onion Ring") &&
                        ingredient.getToken().equals("ONION_RING")
        ));
    }

    @Test
    @DisplayName("Adding Ingredient: Throws EntityAlreadyExistsException when name is taken")
    void add_ShouldThrowException_WhenNameIsTaken() {
        AddEntityRequest entityReq = new AddEntityRequest();
        entityReq.setNamePl("Cebula");
        entityReq.setNameEn("Onion");
        AddIngredientRequest request = new AddIngredientRequest();
        request.setEntity(entityReq);

        when(_ingredientsRepo.isNameTaken("Cebula", "Onion")).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> _ingredientsServices.add(request));
        verify(_ingredientsRepo, never()).save(any());
    }

    @Test
    @DisplayName("Adding Ingredient: Throws IllegalStateException when not all allergens are found")
    void add_ShouldThrowException_WhenAllergensNotFound() {
        AddEntityRequest entityReq = new AddEntityRequest();
        entityReq.setNamePl("Sól");
        entityReq.setNameEn("Salt");
        AddIngredientRequest request = new AddIngredientRequest();
        request.setEntity(entityReq);
        request.setAllergenTokens(Set.of("GLUTEN", "LACTOSE"));

        when(_ingredientsRepo.isNameTaken(anyString(), anyString())).thenReturn(false);
        when(_allergensRepo.findAllergens(anyList())).thenReturn(List.of(new Allergens()));

        assertThrows(IllegalStateException.class, () -> _ingredientsServices.add(request));
    }

    @Test
    @DisplayName("Removing an ingredient: Success - should anonymize data (Soft Delete) and deactivate associated dishes")
    void remove_ShouldSoftDeleteIngredient_AndDeactivateDishes() {
        String token = "TOMATO";
        UUID ingredientId = UUID.randomUUID();

        Ingredients ingredient = new Ingredients();
        ingredient.setId(ingredientId);
        ingredient.setToken(token);
        ingredient.setNameEn("Tomato");
        ingredient.setNamePl("Pomidor");

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
        assertEquals("Tomato is deleted", dish.getUnavailableReason());
        verify(_dishRepo).save(dish);
    }
}
