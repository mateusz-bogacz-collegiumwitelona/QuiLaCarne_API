package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.AddIngredientRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void add_ShouldReturnCreated_WhenRepositorySucceeds() {
        AddEntityRequest entityReq = new AddEntityRequest();
        entityReq.setNamePl("Cebula");
        entityReq.setNameEn("Onion");

        AddIngredientRequest request = new AddIngredientRequest();
        request.setEntity(entityReq);
        request.setAllergenTokens(Set.of("GLUTEN"));

        Allergens mockAllergen = new Allergens();
        mockAllergen.setToken("GLUTEN");
        List<Allergens> foundAllergens = List.of(mockAllergen);

        when(_allergensRepo.findAllergens(anyList())).thenReturn(foundAllergens);

        ResultHandler<Void> result = _ingredientsServices.add(request);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());
        assertEquals("Ingredient created successful", result.getMessage());

        verify(_allergensRepo, times(1)).findAllergens(anyList());
        verify(_ingredientsRepo, times(1)).add(entityReq, foundAllergens);
    }
}
