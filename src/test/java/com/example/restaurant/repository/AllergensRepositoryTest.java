package com.example.restaurant.repository;

import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.jpa.IJpaAllergensRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AllergensRepositoryTest {
    @Mock
    private IJpaAllergensRepository _jpaAllergensRepo;

    @InjectMocks
    private AllergensRepository _allergensRepo;

    @Test
    @DisplayName("Find allergens: Return list of allergens")
    void findAllergens_ShouldReturnListOfAllergens() {
        List<String> tokens = List.of("GLUTEN", "LACTOSE");

        Allergens gluten = new Allergens();
        gluten.setToken("GLUTEN");

        when(_jpaAllergensRepo.findByTokenIn(tokens))
                .thenReturn(List.of(gluten));

        List<Allergens> result = _allergensRepo.findAllergens(tokens);

        assertEquals(1, result.size());
        verify(_jpaAllergensRepo, times(1)).findByTokenIn(tokens);
    }

    @Test
    @DisplayName("Find allergens: Return empty list when no tokens")
    void findAllergens_ShouldReturnEmptyList_WhenTokensListIsEmpty() {
        List<Allergens> result = _allergensRepo.findAllergens(List.of());

        assertTrue(result.isEmpty());
        verify(_jpaAllergensRepo, never()).findByTokenIn(anyList());
    }

    @Test
    @DisplayName("Find allergens: Return empty list when tokens is null")
    void findAllergens_ShouldReturnEmptyList_WhenTokensListIsNull() {
        List<Allergens> result = _allergensRepo.findAllergens(null);

        assertTrue(result.isEmpty());
        verify(_jpaAllergensRepo, never()).findByTokenIn(any());
    }
}
