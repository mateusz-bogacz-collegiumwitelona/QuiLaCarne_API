package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngredientsServicesTest {
    @Mock
    private IIngredientsRepository _ingredientsRepo;

    @Mock
    private IAllergensRepository _allergensRepo;

    @InjectMocks
    private IngredientsServices _ingredientsServices;

    @Test
    void add_ShouldReturnCreated_WhenDataIsCorrect() {
        AddEntityRequest entityReq = new AddEntityRequest();
        entityReq.setNamePl("Cebula");
        entityReq.setNameEn("Onion");

        AddIngredientRequest request = new AddIngredientRequest();
        request.setEntity(entityReq);
        request.setAllergenTokens(Set.of("GLUTEN"));

        Allergens mockAllergen = new Allergens();
        mockAllergen.setToken("GLUTEN");
        List<Allergens> foundAllergens = List.of(mockAllergen);

        when(_ingredientsRepo.isNameTaken("Cebula", "Onion")).thenReturn(false);
        when(_allergensRepo.findAllergens(anyList())).thenReturn(foundAllergens);


        ResultHandler<Void> result = _ingredientsServices.add(request);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());

        verify(_ingredientsRepo, times(1)).save(argThat(ingredient ->
                ingredient.getNamePl().equals("Cebula") &&
                        ingredient.getNameEn().equals("Onion") &&
                        ingredient.getToken().equals("ONION") &&
                        ingredient.getAllergens().size() == 1
        ));
    }

    @Test
    void add_ShouldThrowException_WhenNameIsTaken() {
        AddEntityRequest entityReq = new AddEntityRequest();
        entityReq.setNamePl("Cebula");
        entityReq.setNameEn("Onion");

        AddIngredientRequest request = new AddIngredientRequest();
        request.setEntity(entityReq);

        when(_ingredientsRepo.isNameTaken("Cebula", "Onion")).thenReturn(true);

        EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class, () ->
                _ingredientsServices.add(request)
        );

        assertEquals("Ingredient already exists", exception.getMessage());
        verify(_allergensRepo, never()).findAllergens(anyList());
        verify(_ingredientsRepo, never()).save(any());
    }

    @Test
    void add_ShouldThrowException_WhenAllergensNotFound() {
        AddEntityRequest entityReq = new AddEntityRequest();
        entityReq.setNamePl("Cebula");
        entityReq.setNameEn("Onion");

        AddIngredientRequest request = new AddIngredientRequest();
        request.setEntity(entityReq);
        request.setAllergenTokens(Set.of("GLUTEN", "LACTOSE"));

        Allergens mockAllergen = new Allergens();
        mockAllergen.setToken("GLUTEN");
        List<Allergens> foundAllergens = List.of(mockAllergen);

        when(_ingredientsRepo.isNameTaken("Cebula", "Onion")).thenReturn(false);
        when(_allergensRepo.findAllergens(anyList())).thenReturn(foundAllergens);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _ingredientsServices.add(request)
        );

        assertEquals("One or more allergens not found", exception.getMessage());
        verify(_ingredientsRepo, never()).save(any());
    }

    @Test
    void add_ShouldCreateIngredientWithoutAllergens_WhenTokensAreNullOrEmpty() {
        AddEntityRequest entityReq = new AddEntityRequest();
        entityReq.setNamePl("Woda");
        entityReq.setNameEn("Water");

        AddIngredientRequest request = new AddIngredientRequest();
        request.setEntity(entityReq);
        request.setAllergenTokens(null);

        when(_ingredientsRepo.isNameTaken(anyString(), anyString())).thenReturn(false);
        when(_allergensRepo.findAllergens(any())).thenReturn(new java.util.ArrayList<>());

        ResultHandler<Void> result = _ingredientsServices.add(request);

        assertTrue(result.isSuccess());
        verify(_ingredientsRepo).save(argThat(i -> i.getAllergens().isEmpty()));
    }
}
