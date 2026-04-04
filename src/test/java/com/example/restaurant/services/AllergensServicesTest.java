package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.AddEntityRequest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_PL));

        Allergens allergen = new Allergens();
        allergen.setToken(TestConstants.TOKEN_GLUTEN);
        allergen.setNamePl(TestConstants.FAKE_ALLERGEN_PL);
        allergen.setNameEn(TestConstants.FAKE_ALLERGEN_EN);

        when(_allergenRepo.findAll()).thenReturn(List.of(allergen));

        List<EntityResponse> result = _allergensServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals(TestConstants.TOKEN_GLUTEN, result.getFirst().getToken());
        assertEquals(TestConstants.FAKE_ALLERGEN_PL, result.getFirst().getName());
    }

    @Test
    @DisplayName("getDictionary: Returns mapped elements with English names when language is not pl")
    void getDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_EN));

        Allergens allergen = new Allergens();
        allergen.setToken(TestConstants.TOKEN_NUTS);
        allergen.setNamePl(TestConstants.FAKE_ALLERGEN_PL);
        allergen.setNameEn(TestConstants.FAKE_ALLERGEN_EN);

        when(_allergenRepo.findAll()).thenReturn(List.of(allergen));

        List<EntityResponse> result = _allergensServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals(TestConstants.TOKEN_NUTS, result.getFirst().getToken());
        assertEquals(TestConstants.FAKE_ALLERGEN_EN, result.getFirst().getName());
    }

    @Test
    @DisplayName("Add: Should save allergen when data is correct")
    void add_ShouldSaveAllergen_WhenDataIsCorrect() {
        AddEntityRequest request = new AddEntityRequest();
        request.setNamePl(TestConstants.FAKE_ALLERGEN_PL);
        request.setNameEn(TestConstants.FAKE_ALLERGEN_EN);

        when(_allergenRepo.isNameTaken(anyString(), anyString())).thenReturn(false);

        assertDoesNotThrow(() -> _allergensServices.add(request));

        verify(_allergenRepo, times(1)).save(argThat(allergen ->
                allergen.getNamePl().equals(TestConstants.FAKE_ALLERGEN_PL) &&
                        allergen.getNameEn().equals(TestConstants.FAKE_ALLERGEN_EN) &&
                        allergen.getToken().equals(
                                TestConstants.FAKE_ALLERGEN_EN.toUpperCase().replace(" ", "_")
                        )
        ));
    }
}