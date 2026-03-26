package com.example.restaurant.repository;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.jpa.IJpaIngredientsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngredientsRepositoryTest {

    @Mock
    private IJpaIngredientsRepository _jpaIngredientsRepo;

    @InjectMocks
    private IngredientsRepository _ingredientsRepository;

    @Test
    void add_ShouldSaveIngredientWithGeneratedToken_WhenNameIsUnique() {
        AddEntityRequest request = new AddEntityRequest();
        request.setNamePl("Czerwona Cebula");
        request.setNameEn("Red Onion");

        when(_jpaIngredientsRepo.findByNamePl(request.getNamePl())).thenReturn(Optional.empty()); //
        when(_jpaIngredientsRepo.findByNameEn(request.getNameEn())).thenReturn(Optional.empty()); //

        _ingredientsRepository.add(request);

        verify(_jpaIngredientsRepo, times(1)).saveAndFlush(argThat(ingredient ->
                ingredient.getNamePl().equals("Czerwona Cebula") &&
                        ingredient.getNameEn().equals("Red Onion") &&
                        ingredient.getToken().equals("RED_ONION")));
    }

    @Test
    void add_ShouldThrowException_WhenPolishNameExists() {
        AddEntityRequest request = new AddEntityRequest();
        request.setNamePl("Cebula");
        request.setNameEn("Onion");

        when(_jpaIngredientsRepo.findByNamePl(request.getNamePl())).thenReturn(Optional.of(new Ingredients()));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _ingredientsRepository.add(request) //
        );

        assertEquals("Name already exists", exception.getMessage());
        verify(_jpaIngredientsRepo, never()).saveAndFlush(any());
    }

    @Test
    void add_ShouldThrowException_WhenEnglishNameExists() {
        AddEntityRequest request = new AddEntityRequest();
        request.setNamePl("Cebula");
        request.setNameEn("Onion");

        when(_jpaIngredientsRepo.findByNamePl(request.getNamePl())).thenReturn(Optional.empty()); //
        when(_jpaIngredientsRepo.findByNameEn(request.getNameEn())).thenReturn(Optional.of(new Ingredients())); //

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _ingredientsRepository.add(request)
        );

        assertEquals("Name already exists", exception.getMessage());
        verify(_jpaIngredientsRepo, never()).saveAndFlush(any());
    }
}