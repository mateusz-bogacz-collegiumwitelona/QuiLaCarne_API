package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.jpa.IJpaAllergensRepository;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Find allergens: Return list of allergens")
    void findAllergens_ShouldReturnListOfAllergens() {
        List<String> tokens = List.of(TestConstants.TOKEN_GLUTEN, TestConstants.TOKEN_LACTOSE);

        Allergens gluten = new Allergens();
        gluten.setToken(TestConstants.TOKEN_GLUTEN);

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

    @Test
    @DisplayName("findAll: Returns all allergens")
    void findAll_ShouldReturnAllAllergens() {
        List<Allergens> expectedAllergens = List.of(new Allergens());

        when(_jpaAllergensRepo.findAll()).thenReturn(expectedAllergens);

        List<Allergens> result = _allergensRepo.findAll();

        assertEquals(expectedAllergens, result);
        verify(_jpaAllergensRepo).findAll();
    }

    @Test
    @DisplayName("isNameTaken: Should return true and short-circuit when PL name exists")
    void isNameTaken_ShouldReturnTrue_WhenPlNameExists() {
        when(_jpaAllergensRepo.findByNamePl(TestConstants.FAKE_ALLERGEN_PL))
                .thenReturn(java.util.Optional.of(new Allergens()));

        boolean result = _allergensRepo.isNameTaken(TestConstants.FAKE_ALLERGEN_PL, TestConstants.FAKE_ALLERGEN_EN);

        assertTrue(result);
        verify(_jpaAllergensRepo, times(1)).findByNamePl(TestConstants.FAKE_ALLERGEN_PL);
        verify(_jpaAllergensRepo, never()).findByNameEn(anyString());
    }

    @Test
    @DisplayName("isNameTaken: Should return true when EN name exists")
    void isNameTaken_ShouldReturnTrue_WhenEnNameExists() {
        when(_jpaAllergensRepo.findByNamePl(TestConstants.FAKE_ALLERGEN_PL))
                .thenReturn(java.util.Optional.empty());
        when(_jpaAllergensRepo.findByNameEn(TestConstants.FAKE_ALLERGEN_EN))
                .thenReturn(java.util.Optional.of(new Allergens()));

        boolean result = _allergensRepo.isNameTaken(TestConstants.FAKE_ALLERGEN_PL, TestConstants.FAKE_ALLERGEN_EN);

        assertTrue(result);
        verify(_jpaAllergensRepo, times(1)).findByNamePl(TestConstants.FAKE_ALLERGEN_PL);
        verify(_jpaAllergensRepo, times(1)).findByNameEn(TestConstants.FAKE_ALLERGEN_EN);
    }

    @Test
    @DisplayName("isNameTaken: Should return false when both names are available")
    void isNameTaken_ShouldReturnFalse_WhenBothNamesAreAvailable() {
        when(_jpaAllergensRepo.findByNamePl(TestConstants.FAKE_ALLERGEN_PL))
                .thenReturn(java.util.Optional.empty());
        when(_jpaAllergensRepo.findByNameEn(TestConstants.FAKE_ALLERGEN_EN))
                .thenReturn(java.util.Optional.empty());

        boolean result = _allergensRepo.isNameTaken(TestConstants.FAKE_ALLERGEN_PL, TestConstants.FAKE_ALLERGEN_EN);

        assertFalse(result);
    }

    @Test
    @DisplayName("save: Should call JPA save")
    void save_ShouldCallJpaSave() {
        Allergens allergen = new Allergens();

        _allergensRepo.save(allergen);

        verify(_jpaAllergensRepo, times(1)).save(allergen);
    }

    @Test
    @DisplayName("findByToken: Should return allergen when exists")
    void findByToken_ShouldReturnAllergen_WhenExists() {
        String token = TestConstants.TOKEN_GLUTEN;
        Allergens allergen = new Allergens();
        allergen.setToken(token);

        when(_jpaAllergensRepo.findByToken(token)).thenReturn(java.util.Optional.of(allergen));

        Allergens result = _allergensRepo.findByToken(token);

        assertNotNull(result);
        assertEquals(token, result.getToken());
        verify(_jpaAllergensRepo, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("findByToken: Should throw EntityNotFoundException when allergen does not exist")
    void findByToken_ShouldThrowException_WhenNotFound() {
        String token = "NON_EXISTING";
        when(_jpaAllergensRepo.findByToken(token)).thenReturn(java.util.Optional.empty());

        com.example.restaurant.exceptions.EntityNotFoundException exception = assertThrows(
                com.example.restaurant.exceptions.EntityNotFoundException.class,
                () -> _allergensRepo.findByToken(token)
        );

        assertEquals("Allergen not found", exception.getMessage());
        verify(_jpaAllergensRepo, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("count: Should return total number of allergens")
    void count_ShouldReturnTotalCount() {
        when(_jpaAllergensRepo.count()).thenReturn(15L);
        long result = _allergensRepo.count();
        assertEquals(15L, result);
        verify(_jpaAllergensRepo, times(1)).count();
    }
}
