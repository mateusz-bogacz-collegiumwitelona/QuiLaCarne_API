package com.example.restaurant.services;

import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AllergensServicesTest {

    @Mock
    private IAllergensRepository _allergenRepo;

    @InjectMocks
    private AllergensServices _allergensServices;

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("getDictionary: Returns empty list when repository returns empty")
    void getDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
        when(_allergenRepo.findAll()).thenReturn(new ArrayList<>());

        List<EntityResponse> result = _allergensServices.getDictionary();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getDictionary: Returns mapped elements with Polish names when language is pl")
    void getDictionary_ShouldReturnPolishNames_WhenLanguageIsPl() {
        LocaleContextHolder.setLocale(new Locale("pl"));

        Allergens allergen = new Allergens();
        allergen.setToken("GLUTEN");
        allergen.setNamePl("Gluten PL");
        allergen.setNameEn("Gluten EN");

        when(_allergenRepo.findAll()).thenReturn(List.of(allergen));

        List<EntityResponse> result = _allergensServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals("GLUTEN", result.getFirst().getToken());
        assertEquals("Gluten PL", result.getFirst().getName());
    }

    @Test
    @DisplayName("getDictionary: Returns mapped elements with English names when language is not pl")
    void getDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
        LocaleContextHolder.setLocale(new Locale("en"));

        Allergens allergen = new Allergens();
        allergen.setToken("NUTS");
        allergen.setNamePl("Orzechy PL");
        allergen.setNameEn("Nuts EN");

        when(_allergenRepo.findAll()).thenReturn(List.of(allergen));

        List<EntityResponse> result = _allergensServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals("NUTS", result.getFirst().getToken());
        assertEquals("Nuts EN", result.getFirst().getName());
    }
}