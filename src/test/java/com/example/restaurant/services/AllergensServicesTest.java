package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.enums.WebSocketEventType;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

    @Mock
    private IIngredientsRepository _ingredientsRepo;

    @Mock
    private NotificationServices _notification;

    @InjectMocks
    private AllergensServices _allergensServices;

    @Spy
    private SyncMapper _syncMapper = Mappers.getMapper(SyncMapper.class);

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("getDictionary: Returns empty list when repository returns empty")
    void getDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
        when(_allergenRepo.findAll()).thenReturn(new ArrayList<>());

        DictionaryResponse result = _allergensServices.getDictionary();

        assertTrue(result.getItem().isEmpty());
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

        DictionaryResponse result = _allergensServices.getDictionary();

        assertEquals(1, result.getItem().size());
        assertEquals(TestConstants.TOKEN_GLUTEN, result.getItem().getFirst().getToken());
        assertEquals(TestConstants.FAKE_ALLERGEN_PL, result.getItem().getFirst().getName());
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

        DictionaryResponse result = _allergensServices.getDictionary();

        assertEquals(1, result.getItem().size());
        assertEquals(TestConstants.TOKEN_NUTS, result.getItem().getFirst().getToken());
        assertEquals(TestConstants.FAKE_ALLERGEN_EN, result.getItem().getFirst().getName());
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

        verify(_notification, times(1)).sendEventToTopic(
                eq("/dictionary/allergens"),
                argThat(event ->
                        event != null &&
                                event.getEventType() == WebSocketEventType.CREATED &&
                                "ALLERGEN".equals(event.getEntityType()) &&
                                event.getPayload() != null
                )
        );
    }

    @Test
    @DisplayName("Remove: Should soft delete allergen and remove it from associated ingredients")
    void remove_ShouldSoftDeleteAllergen_AndRemoveFromIngredients() {
        String token = TestConstants.TOKEN_GLUTEN;
        Allergens allergen = new Allergens();
        allergen.setToken(token);
        allergen.setNameEn(TestConstants.FAKE_ALLERGEN_EN);
        allergen.setNamePl(TestConstants.FAKE_ALLERGEN_PL);

        com.example.restaurant.models.Ingredients ingredient1 = new com.example.restaurant.models.Ingredients();
        ingredient1.getAllergens().add(allergen);

        com.example.restaurant.models.Ingredients ingredient2 = new com.example.restaurant.models.Ingredients();
        ingredient2.getAllergens().add(allergen);

        allergen.getIngredients().add(ingredient1);
        allergen.getIngredients().add(ingredient2);

        when(_allergenRepo.findByToken(token)).thenReturn(allergen);

        assertDoesNotThrow(() -> _allergensServices.remove(token));
        assertFalse(ingredient1.getAllergens().contains(allergen));
        assertFalse(ingredient2.getAllergens().contains(allergen));
        verify(_ingredientsRepo, times(1)).save(ingredient1);
        verify(_ingredientsRepo, times(1)).save(ingredient2);

        assertTrue(allergen.getToken().startsWith("DELETED_"));
        assertTrue(allergen.getNameEn().startsWith("DELETED_"));
        assertNotNull(allergen.getDeletedAt());
        verify(_allergenRepo, times(1)).save(allergen);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/dictionary/allergens"),
                argThat(event ->
                        event != null &&
                                event.getEventType() == WebSocketEventType.DELETED &&
                                "ALLERGEN".equals(event.getEntityType()) &&
                                token.equals(event.getToken()) &&
                                event.getPayload() == null
                )
        );
    }
}