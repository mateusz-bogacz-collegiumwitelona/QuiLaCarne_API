package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.payload.DishPayload;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.dto.response.SyncDictionaryResponse;
import com.example.restaurant.enums.WebSocketEventType;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DishServicesTest {
    @Mock
    private IDishRepository _dishRepo;

    @Mock
    private DishMapper _dishMapper;

    @Mock
    private IIngredientsRepository _ingredientsRepo;

    @Mock
    private S3StorageService _s3Services;

    @Mock
    private NotificationServices _notification;

    @InjectMocks
    private DishServices _dishServices;

    @Spy
    private SyncMapper _syncMapper = Mappers.getMapper(SyncMapper.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(_dishServices, "s3Endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(_dishServices, "s3BucketName", "restaurant-images");
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("get menu: should use the current language, map the dishes and add the URL to S3")
    void getMenu_ShouldUseCurrentLocale_MapDishes_AndAppendS3Url() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        Dishes mockDish = new Dishes();
        Page<Dishes> mockPage = new PageImpl<>(List.of(mockDish), PageRequest.of(0, 10), 1);
        DishListResponse dishResponse = DishListResponse.builder().imageUrl("steak.jpg").build();

        when(_dishRepo.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(mockDish, "en")).thenReturn(dishResponse);

        PagedResult<DishListResponse> result = _dishServices.getMenu(new DishFilterRequest(), new PaggedRequest());

        assertNotNull(result);
        assertEquals("http://localhost:9000/restaurant-images/steak.jpg",
                result.getItems().getFirst().getImageUrl()
        );
    }

    @Test
    @DisplayName("Get menu: It should not modify the URL if it starts with 'http'")
    void getMenu_ShouldNotModifyUrl_WhenItAlreadyStartsHttp() {
        Page<Dishes> mockPage = new PageImpl<>(List.of(new Dishes()));
        DishListResponse dishResponse = DishListResponse.builder().imageUrl("https://external.com/img.jpg").build();

        when(_dishRepo.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(any(), anyString())).thenReturn(dishResponse);

        PagedResult<DishListResponse> result = _dishServices.getMenu(new DishFilterRequest(), new PaggedRequest());

        assertEquals("https://external.com/img.jpg", result.getItems().getFirst().getImageUrl());
    }

    @Test
    @DisplayName("Get Menu: It should handle a blank results page correctly")
    void getMenu_ShouldHandleEmptyPage() {
        when(_dishRepo.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(Page.empty());

        PagedResult<DishListResponse> result = _dishServices.getMenu(new DishFilterRequest(), new PaggedRequest());

        assertTrue(result.getItems().isEmpty());
    }

    @Test
    @DisplayName("remove: should disable the availability of the dish, " +
            "add the reason and date of deletion (soft delete)")
    void remove_ShouldMarkDishAsDeleted_AndSaveToRepository() {
        Dishes dish = new Dishes();
        dish.setToken(TestConstants.FAKE_DISH_TOKEN);
        dish.setAvailable(true);
        dish.setUnavailableReason(null);
        dish.setDeletedAt(null);

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);

        _dishServices.remove(TestConstants.FAKE_DISH_TOKEN);

        assertNull(dish.getImageUrl());
        verify(_s3Services, times(1)).deleteFile(any());

        assertFalse(dish.isAvailable());
        assertEquals("Dish is deleted", dish.getUnavailableReason());
        assertNotNull(dish.getDeletedAt());

        verify(_dishRepo, times(1)).findByToken(TestConstants.FAKE_DISH_TOKEN);
        verify(_dishRepo, times(1)).save(dish);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/menu/dishes"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.DELETED &&
                                event.getEntityType().equals("DISH") &&
                                event.getToken().equals(TestConstants.FAKE_DISH_TOKEN) &&
                                event.getPayload() == null
                )
        );
    }

    @Test
    @DisplayName("changeAvailable: should restore the availability " +
            "of the dish and remove the reason for its unavailability")
    void changeAvailable_ShouldSetAvailable_AndClearReason() {
        Dishes dish = new Dishes();
        dish.setToken(TestConstants.FAKE_DISH_TOKEN);
        dish.setAvailable(false);
        dish.setUnavailableReason("Zepsuty piec");

        ChangeDishAvailableRequest request = new ChangeDishAvailableRequest();
        request.setToken(TestConstants.FAKE_DISH_TOKEN);
        request.setAvailable(true);
        request.setUnavailableReason("Ten tekst i tak zostanie zignorowany");

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);

        _dishServices.changeAvailable(request);

        assertTrue(dish.isAvailable());
        assertNull(dish.getUnavailableReason());

        verify(_dishRepo, times(1)).findByToken(TestConstants.FAKE_DISH_TOKEN);
        verify(_dishRepo, times(1)).save(dish);
        verify(_notification, times(1)).sendEventToTopic(
                eq("/menu/dishes"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.UPDATED &&
                                event.getEntityType().equals("DISH") &&
                                event.getToken().equals(TestConstants.FAKE_DISH_TOKEN) &&
                                event.getPayload() != null &&
                                ((DishPayload) event.getPayload()).isAvailable()
                )
        );
    }

    @Test
    @DisplayName("changeAvailable: should disable accessibility and set custom reason ")
    void changeAvailable_ShouldSetUnavailable_AndSetCustomReason() {
        Dishes dish = new Dishes();
        dish.setToken(TestConstants.FAKE_DISH_TOKEN);
        dish.setAvailable(true);

        ChangeDishAvailableRequest request = new ChangeDishAvailableRequest();
        request.setToken(TestConstants.FAKE_DISH_TOKEN);
        request.setAvailable(false);
        request.setUnavailableReason("   Brak świeżej bazylii   ");

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);

        _dishServices.changeAvailable(request);

        assertFalse(dish.isAvailable());
        assertEquals("Brak świeżej bazylii", dish.getUnavailableReason());

        verify(_dishRepo, times(1)).save(dish);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/menu/dishes"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.UPDATED &&
                                event.getEntityType().equals("DISH") &&
                                event.getToken().equals(TestConstants.FAKE_DISH_TOKEN) &&
                                event.getPayload() != null &&
                                !((DishPayload) event.getPayload()).isAvailable() &&
                                "Brak świeżej bazylii".equals(((DishPayload) event.getPayload()).getUnavailableReason())
                )
        );
    }

    @Test
    @DisplayName("changeAvailable: should disable accessibility and set default reason when null/empty string is sent")
    void changeAvailable_ShouldSetUnavailable_AndSetDefaultReason_WhenReasonIsNullOrBlank() {
        Dishes dish = new Dishes();
        dish.setToken(TestConstants.FAKE_DISH_TOKEN);
        dish.setAvailable(true);

        ChangeDishAvailableRequest request = new ChangeDishAvailableRequest();
        request.setToken(TestConstants.FAKE_DISH_TOKEN);
        request.setAvailable(false);
        request.setUnavailableReason(null);

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);

        _dishServices.changeAvailable(request);

        assertFalse(dish.isAvailable());
        assertEquals("Brak składników", dish.getUnavailableReason());

        verify(_dishRepo, times(1)).save(dish);
        verify(_notification, times(1)).sendEventToTopic(
                eq("/menu/dishes"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.UPDATED &&
                                event.getEntityType().equals("DISH") &&
                                event.getToken().equals(TestConstants.FAKE_DISH_TOKEN) &&
                                event.getPayload() != null &&
                                "Brak składników".equals(((DishPayload) event.getPayload()).getUnavailableReason())
                )
        );
    }

    @Test
    @DisplayName("edit: Should update basic dish properties (name, price) and save without interacting with S3")
    void edit_ShouldUpdateBasicProperties_AndSave() {
        EditDishRequest request = new EditDishRequest();
        request.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        request.setNewName(TestConstants.FAKE_DISH_NAME);
        request.setPrice(1500);

        Dishes dish = new Dishes();
        dish.setToken(TestConstants.FAKE_DISH_TOKEN);
        dish.setName("Old Name");
        dish.setPrice(1000);

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);

        _dishServices.edit(request);

        assertEquals(TestConstants.FAKE_DISH_NAME, dish.getName(), "Name should be updated and trimmed");
        assertEquals(1500, dish.getPrice(), "Price should be updated");
        verify(_dishRepo, times(1)).save(dish);
        verifyNoInteractions(_s3Services);
        verifyNoInteractions(_ingredientsRepo);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/menu/dishes"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.UPDATED &&
                                event.getEntityType().equals("DISH") &&
                                event.getToken().equals(TestConstants.FAKE_DISH_TOKEN) &&
                                event.getPayload() != null
                )
        );
    }

    @Test
    @DisplayName("edit: Should update category and ingredients successfully")
    void edit_ShouldUpdateCategoryAndIngredients() {
        EditDishRequest request = new EditDishRequest();
        request.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        request.setCategoryToken(TestConstants.FAKE_DISH_CATEGORY);
        request.setIngredientTokens(List.of(TestConstants.INGREDIENT_PL));

        Dishes dish = new Dishes();
        dish.setToken(TestConstants.FAKE_DISH_TOKEN);

        DishesCategories category = new DishesCategories();
        category.setToken(TestConstants.FAKE_DISH_CATEGORY);

        Ingredients ingredient = new Ingredients();
        ingredient.setToken(TestConstants.INGREDIENT_PL);

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);
        when(_dishRepo.findCategoryByToken(TestConstants.FAKE_DISH_CATEGORY)).thenReturn(category);
        when(_ingredientsRepo.findByToken(TestConstants.INGREDIENT_PL)).thenReturn(ingredient);

        _dishServices.edit(request);

        assertEquals(category, dish.getCategory());
        assertEquals(1, dish.getIngredients().size());
        assertTrue(dish.getIngredients().contains(ingredient));
        verify(_dishRepo, times(1)).save(dish);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/menu/dishes"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.UPDATED &&
                                event.getEntityType().equals("DISH") &&
                                event.getToken().equals(TestConstants.FAKE_DISH_TOKEN) &&
                                event.getPayload() != null &&
                                TestConstants.FAKE_DISH_CATEGORY.equals(
                                        ((DishPayload) event.getPayload()).getCategoryToken()) &&
                                ((DishPayload) event.getPayload())
                                        .getIngredientTokens().contains(TestConstants.INGREDIENT_PL)
                )
        );
    }

    @Test
    @DisplayName("edit: Should delete old photo, upload new photo to S3, and update imageUrl")
    void edit_ShouldUpdatePhotoAndImageUrl() throws IOException {
        EditDishRequest request = new EditDishRequest();
        request.setDishToken(TestConstants.FAKE_DISH_TOKEN);

        MultipartFile mockPhoto = mock(MultipartFile.class);
        when(mockPhoto.isEmpty()).thenReturn(false);
        when(mockPhoto.getOriginalFilename()).thenReturn("steak.png");
        when(mockPhoto.getContentType()).thenReturn("image/png");
        when(mockPhoto.getSize()).thenReturn(1024L);

        InputStream mockInputStream = mock(InputStream.class);
        when(mockPhoto.getInputStream()).thenReturn(mockInputStream);

        request.setPhoto(mockPhoto);

        Dishes dish = new Dishes();
        dish.setToken(TestConstants.FAKE_DISH_TOKEN);
        dish.setImageUrl("old_steak_image.jpg");

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);
        when(_s3Services.generateUniqFileName("steak.png")).thenReturn("new_uuid_steak.png");
        when(_s3Services.uploadFromStream(
                mockInputStream,
                "new_uuid_steak.png",
                "image/png",
                1024L)
        ).thenReturn("new_uuid_steak.png");

        _dishServices.edit(request);

        verify(_s3Services, times(1)).deleteFile("old_steak_image.jpg");
        assertEquals("new_uuid_steak.png", dish.getImageUrl(), "Image URL should be updated");
        verify(_dishRepo, times(1)).save(dish);
        verify(_notification, times(1)).sendEventToTopic(
                eq("/menu/dishes"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.UPDATED &&
                                event.getEntityType().equals("DISH") &&
                                event.getToken().equals(TestConstants.FAKE_DISH_TOKEN) &&
                                event.getPayload() != null &&
                                ((DishPayload) event.getPayload()).getImageUrl().contains("new_uuid_steak.png")
                )
        );
    }

    @Test
    @DisplayName("edit: Should throw RuntimeException when reading photo input stream fails")
    void edit_ShouldThrowRuntimeException_WhenPhotoStreamFails() throws IOException {
        EditDishRequest request = new EditDishRequest();
        request.setDishToken(TestConstants.FAKE_DISH_TOKEN);

        MultipartFile mockPhoto = mock(MultipartFile.class);
        when(mockPhoto.isEmpty()).thenReturn(false);
        when(mockPhoto.getOriginalFilename()).thenReturn("steak.png");
        when(mockPhoto.getInputStream()).thenThrow(new IOException("Stream error"));

        request.setPhoto(mockPhoto);

        Dishes dish = new Dishes();
        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);
        when(_s3Services.generateUniqFileName("steak.png")).thenReturn("new_uuid_steak.png");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> _dishServices.edit(request));

        assertEquals("Could not process photo file", exception.getMessage());
        verify(_dishRepo, never()).save(any());
    }

    @Test
    @DisplayName("add: Should create basic dish with trimmed name, set available to true, and save")
    void add_ShouldCreateBasicDish_AndSave() {
        AddDishRequest request = new AddDishRequest();
        request.setName(TestConstants.FAKE_DISH_NAME);
        request.setPrice(2500);
        request.setCategoryToken(TestConstants.FAKE_DISH_CATEGORY);

        DishesCategories category = new DishesCategories();
        category.setToken(TestConstants.FAKE_DISH_CATEGORY);
        when(_dishRepo.findCategoryByToken(TestConstants.FAKE_DISH_CATEGORY)).thenReturn(category);

        doAnswer(invocation -> {
            Dishes d = invocation.getArgument(0);
            d.setToken("NEW_DISH_TOKEN");
            return null;
        }).when(_dishRepo).save(any(Dishes.class));

        _dishServices.add(request);

        ArgumentCaptor<Dishes> dishCaptor = ArgumentCaptor.forClass(Dishes.class);
        verify(_dishRepo, times(1)).save(dishCaptor.capture());

        Dishes savedDish = dishCaptor.getValue();
        assertEquals(TestConstants.FAKE_DISH_NAME, savedDish.getName(), "Name should be trimmed");
        assertEquals(2500, savedDish.getPrice(), "Price should be mapped correctly");
        assertEquals(category, savedDish.getCategory(), "Category should be mapped");
        assertTrue(savedDish.isAvailable(), "New dish should be available by default");
        assertNull(savedDish.getImageUrl(), "Image URL should be null if no photo was uploaded");
        assertTrue(savedDish.getIngredients().isEmpty(), "Ingredients should be empty");

        verifyNoInteractions(_s3Services);
        verifyNoInteractions(_ingredientsRepo);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/menu/dishes"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.CREATED &&
                                event.getEntityType().equals("DISH") &&
                                "NEW_DISH_TOKEN".equals(event.getToken()) &&
                                event.getPayload() != null &&
                                TestConstants.FAKE_DISH_CATEGORY
                                        .equals(((DishPayload) event.getPayload()).getCategoryToken())
                )
        );
    }

    @Test
    @DisplayName("add: Should correctly map category, ingredients, and upload photo to S3")
    void add_ShouldCreateDishWithIngredientsAndPhoto() throws IOException {
        AddDishRequest request = new AddDishRequest();
        request.setName(TestConstants.FAKE_DISH_NAME);
        request.setPrice(3000);
        request.setCategoryToken(TestConstants.FAKE_DISH_CATEGORY);
        request.setIngredientTokens(List.of(TestConstants.INGREDIENT_EN));

        MultipartFile mockPhoto = mock(MultipartFile.class);
        when(mockPhoto.isEmpty()).thenReturn(false);
        when(mockPhoto.getOriginalFilename()).thenReturn("pizza.png");
        when(mockPhoto.getContentType()).thenReturn("image/png");
        when(mockPhoto.getSize()).thenReturn(2048L);
        InputStream mockInputStream = mock(InputStream.class);
        when(mockPhoto.getInputStream()).thenReturn(mockInputStream);

        request.setPhoto(mockPhoto);

        DishesCategories category = new DishesCategories();
        category.setToken(TestConstants.FAKE_DISH_CATEGORY); // POPRAWKA: Brakowało tokena

        Ingredients cheese = new Ingredients();
        cheese.setToken(TestConstants.INGREDIENT_EN); // POPRAWKA: Brakowało tokena

        when(_dishRepo.findCategoryByToken(TestConstants.FAKE_DISH_CATEGORY)).thenReturn(category);
        when(_ingredientsRepo.findByToken(TestConstants.INGREDIENT_EN)).thenReturn(cheese);
        when(_s3Services.generateUniqFileName("pizza.png")).thenReturn("uuid_pizza.png");
        when(_s3Services.uploadFromStream(
                        mockInputStream,
                        "uuid_pizza.png",
                        "image/png",
                        2048L
                )
        ).thenReturn("uuid_pizza.png");

        doAnswer(invocation -> {
            Dishes d = invocation.getArgument(0);
            d.setToken("NEW_PIZZA_TOKEN");
            return null;
        }).when(_dishRepo).save(any(Dishes.class));

        _dishServices.add(request);

        ArgumentCaptor<Dishes> dishCaptor = ArgumentCaptor.forClass(Dishes.class);
        verify(_dishRepo, times(1)).save(dishCaptor.capture());

        Dishes savedDish = dishCaptor.getValue();
        assertEquals(TestConstants.FAKE_DISH_NAME, savedDish.getName());
        assertEquals(category, savedDish.getCategory());
        assertEquals(1, savedDish.getIngredients().size());
        assertTrue(savedDish.getIngredients().contains(cheese));
        assertEquals("uuid_pizza.png", savedDish.getImageUrl());

        verify(_s3Services, never()).deleteFile(any());

        verify(_notification, times(1)).sendEventToTopic(
                eq("/menu/dishes"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.CREATED &&
                                event.getEntityType().equals("DISH") &&
                                "NEW_PIZZA_TOKEN".equals(event.getToken()) &&
                                event.getPayload() != null &&
                                ((DishPayload) event.getPayload()).getImageUrl().contains("uuid_pizza.png") &&
                                ((DishPayload) event.getPayload())
                                        .getIngredientTokens().contains(TestConstants.INGREDIENT_EN)
                )
        );
    }

    @Test
    @DisplayName("add: Should throw RuntimeException when reading photo input stream fails")
    void add_ShouldThrowRuntimeException_WhenPhotoStreamFails() throws IOException {
        AddDishRequest request = new AddDishRequest();
        request.setName(TestConstants.FAKE_DISH_NAME);
        request.setPrice(3000);
        request.setCategoryToken(TestConstants.FAKE_DISH_CATEGORY);

        MultipartFile mockPhoto = mock(MultipartFile.class);
        when(mockPhoto.isEmpty()).thenReturn(false);
        when(mockPhoto.getOriginalFilename()).thenReturn("pizza.png");
        when(mockPhoto.getInputStream()).thenThrow(new IOException("Stream error"));

        request.setPhoto(mockPhoto);

        when(_dishRepo.findCategoryByToken(TestConstants.FAKE_DISH_CATEGORY)).thenReturn(new DishesCategories());
        when(_s3Services.generateUniqFileName("pizza.png")).thenReturn("uuid_pizza.png");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> _dishServices.add(request));

        assertEquals("Could not process photo file", exception.getMessage());
        verify(_dishRepo, never()).save(any());
    }

    @Test
    @DisplayName("getDictionary: Returns empty list when repository returns empty")
    void getDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
        when(_dishRepo.findAllCategories()).thenReturn(new java.util.ArrayList<>());

        DictionaryResponse result = _dishServices.getDictionary();

        assertTrue(result.getItem().isEmpty());
    }

    @Test
    @DisplayName("getDictionary: Returns mapped elements with Polish names when language is pl")
    void getDictionary_ShouldReturnPolishNames_WhenLanguageIsPl() {
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_PL));

        DishesCategories category = new DishesCategories();
        category.setToken(TestConstants.TOKEN_SOUPS);
        category.setNamePl("Zupy PL");
        category.setNameEn("Soups EN");

        when(_dishRepo.findAllCategories()).thenReturn(java.util.List.of(category));

        DictionaryResponse result = _dishServices.getDictionary();

        assertEquals(1, result.getItem().size());
        assertEquals(TestConstants.TOKEN_SOUPS, result.getItem().getFirst().getToken());
        assertEquals("Zupy PL", result.getItem().getFirst().getName());
    }

    @Test
    @DisplayName("getDictionary: Returns mapped elements with English names when language is not pl")
    void getDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_EN));

        DishesCategories category = new DishesCategories();
        category.setToken(TestConstants.TOKEN_DESSERTS);
        category.setNamePl("Desery PL");
        category.setNameEn("Desserts EN");

        when(_dishRepo.findAllCategories()).thenReturn(java.util.List.of(category));

        DictionaryResponse result = _dishServices.getDictionary();

        assertEquals(1, result.getItem().size());
        assertEquals(TestConstants.TOKEN_DESSERTS, result.getItem().getFirst().getToken());
        assertEquals("Desserts EN", result.getItem().getFirst().getName());
    }

    @Test
    @DisplayName("addCategory: Should save category when data is correct")
    void addCategory_ShouldSaveCategory_WhenDataIsCorrect() {
        AddEntityRequest request = new AddEntityRequest();
        request.setNamePl("Przystawki PL");
        request.setNameEn("Starters EN");


        when(_dishRepo.isCategoryNameTaken(anyString(), anyString())).thenReturn(false);

        assertDoesNotThrow(() -> _dishServices.addCategory(request));

        verify(_dishRepo, times(1)).saveCategory(argThat(category ->
                category.getNamePl().equals("Przystawki PL") &&
                        category.getNameEn().equals("Starters EN") &&
                        category.getToken().equals("STARTERS_EN")
        ));

        verify(_notification, times(1)).sendEventToTopic(
                eq("/dictionary/dish-categories"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.CREATED &&
                                event.getEntityType().equals("DISH_CATEGORY") &&
                                event.getPayload() != null &&
                                "Przystawki PL".equals(((SyncDictionaryResponse) event.getPayload()).getNamePl())
                )
        );
    }

    @Test
    @DisplayName("removeCategory: Should soft delete category and reassign associated dishes to OTHER")
    void removeCategory_ShouldSoftDelete_AndReassignDishesToOther() {
        String tokenToRemove = "SOUPS_TOKEN";

        DishesCategories categoryToRemove = new DishesCategories();
        categoryToRemove.setId(java.util.UUID.randomUUID());
        categoryToRemove.setToken(tokenToRemove);
        categoryToRemove.setNameEn("Soups");
        categoryToRemove.setNamePl("Zupy");

        DishesCategories fallbackCategory = new DishesCategories();
        fallbackCategory.setToken("OTHER");
        fallbackCategory.setNameEn("Other");
        fallbackCategory.setNamePl("Inne");

        Dishes dish1 = new Dishes();
        dish1.setCategory(categoryToRemove);

        Dishes dish2 = new Dishes();
        dish2.setCategory(categoryToRemove);

        List<Dishes> affectedDishes = List.of(dish1, dish2);

        when(_dishRepo.findCategoryByToken(tokenToRemove)).thenReturn(categoryToRemove);
        when(_dishRepo.findCategoryByToken("OTHER")).thenReturn(fallbackCategory);
        when(_dishRepo.findByCategoryId(categoryToRemove.getId())).thenReturn(affectedDishes);

        assertDoesNotThrow(() -> _dishServices.removeCategory(tokenToRemove));

        assertEquals(fallbackCategory, dish1.getCategory());
        assertEquals(fallbackCategory, dish2.getCategory());
        verify(_dishRepo, times(1)).save(dish1);
        verify(_dishRepo, times(1)).save(dish2);

        assertTrue(categoryToRemove.getToken().startsWith("DELETED_"));
        assertTrue(categoryToRemove.getNameEn().startsWith("DELETED_"));
        assertNotNull(categoryToRemove.getDeletedAt());
        verify(_dishRepo, times(1)).saveCategory(categoryToRemove);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/dictionary/dish-categories"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.DELETED &&
                                event.getEntityType().equals("DISH_CATEGORY") &&
                                event.getToken().equals(tokenToRemove) &&
                                event.getPayload() == null
                )
        );
    }
}