package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class IngredientsServicesTest {
    @Mock
    private IIngredientsRepository _ingredientsRepo;

    @InjectMocks
    private IngredientsServices _ingredientsServices;

    @Test
    void add_ShouldReturnCreated_WhenRepositorySucceeds() {
        AddEntityRequest request = new AddEntityRequest();
        request.setNamePl("Cebula");
        request.setNameEn("Onion");

        ResultHandler<Void> result = _ingredientsServices.add(request);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());
        assertEquals("Ingredient created successful", result.getMessage());

        verify(_ingredientsRepo, times(1)).add(request);
    }
}
