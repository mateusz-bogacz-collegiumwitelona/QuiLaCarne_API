package com.example.restaurant.repository;

import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.jpa.IJpaIngredientsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngredientsRepositoryTest {

    @Mock
    private IJpaIngredientsRepository _jpaIngredientsRepo;

    @InjectMocks
    private IngredientsRepository _ingredientsRepository;

    @Test
    void save_ShouldCallJpaSave() {
        Ingredients ingredient = new Ingredients();
        _ingredientsRepository.save(ingredient);

        verify(_jpaIngredientsRepo, times(1)).save(ingredient);
    }

    @Test
    void isNameTaken_ShouldReturnTrue_WhenPlNameExists() {
        when(_jpaIngredientsRepo.findByNamePl("Cebula")).thenReturn(Optional.of(new Ingredients()));

        boolean result = _ingredientsRepository.isNameTaken("Cebula", "Onion");

        assertTrue(result);
    }

    @Test
    void isNameTaken_ShouldReturnTrue_WhenEnNameExists() {
        when(_jpaIngredientsRepo.findByNamePl("Cebula")).thenReturn(Optional.empty());
        when(_jpaIngredientsRepo.findByNameEn("Onion")).thenReturn(Optional.of(new Ingredients()));

        boolean result = _ingredientsRepository.isNameTaken("Cebula", "Onion");

        assertTrue(result);
    }

    @Test
    void isNameTaken_ShouldReturnFalse_WhenNamesAreFree() {
        when(_jpaIngredientsRepo.findByNamePl("Cebula")).thenReturn(Optional.empty());
        when(_jpaIngredientsRepo.findByNameEn("Onion")).thenReturn(Optional.empty());

        boolean result = _ingredientsRepository.isNameTaken("Cebula", "Onion");

        assertFalse(result);
    }
}