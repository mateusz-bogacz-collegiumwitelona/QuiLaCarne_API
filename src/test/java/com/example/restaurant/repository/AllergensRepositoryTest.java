package com.example.restaurant.repository;

import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.jpa.IJpaAllergensRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AllergensRepositoryTest {
    @Mock
    private IJpaAllergensRepository _jpaAllergensRepo;

    @InjectMocks
    private AllergensRepository _allergensRepo;

    @Test
    void findAllergens_ShouldReturnListOfAllergens_WhenAllTokensFound() {
        List<String> tokens = List.of("GLUTEN", "LACTOSE");

        Allergens gluten = new Allergens();
        gluten.setToken("GLUTEN");

        Allergens lactose = new Allergens();
        lactose.setToken("LACTOSE");

        when(_jpaAllergensRepo.findByTokenIn(tokens))
                .thenReturn(List.of(gluten, lactose));


        List<Allergens> result = _allergensRepo.findAllergens(tokens);

        assertEquals(2, result.size());
        verify(_jpaAllergensRepo, times(1)).findByTokenIn(tokens);
    }

    @Test
    void findAllergens_ShouldThrowException_WhenNotAllAllergensFound() {
        List<String> tokens = List.of("GLUTEN", "LACTOSE");

        Allergens gluten = new Allergens();
        gluten.setToken("GLUTEN");

        when(_jpaAllergensRepo.findByTokenIn(tokens))
                .thenReturn(List.of(gluten));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _allergensRepo.findAllergens(tokens)
        );

        assertEquals("One or more allergens not found", exception.getMessage());
    }

    @Test
    void findAllergens_ShouldReturnEmptyList_WhenTokensListIsEmpty() {
        List<Allergens> result = _allergensRepo.findAllergens(List.of());

        assertTrue(result.isEmpty());
        verify(_jpaAllergensRepo, never()).findByTokenIn(anyList());
    }
}
