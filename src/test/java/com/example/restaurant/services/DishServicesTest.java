package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.ChangeDishAvailableRequest;
import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.EditDishRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @InjectMocks
    private DishServices _dishServices;

    @Test
    @DisplayName("get menu: should use the current language, map the dishes and add the URL to S3")
    void getMenu_ShouldUseCurrentLocale_MapDishes_AndAppendS3Url() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        ReflectionTestUtils.setField(_dishServices, "s3Endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(_dishServices, "s3BucketName", "restaurant-images");

        Dishes mockDish = new Dishes();
        Page<Dishes> mockPage = new PageImpl<>(List.of(mockDish), PageRequest.of(0, 10), 1);
        DishListResponse dishResponse = DishListResponse.builder().imageUrl("steak.jpg").build();

        when(_dishRepo.findAllDishes(any(), any())).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(mockDish, "en")).thenReturn(dishResponse);

        PagedResult<DishListResponse> result = _dishServices.getMenu(new DishFilterRequest(), new PaggedRequest());

        assertNotNull(result);
        assertEquals("http://localhost:9000/restaurant-images/steak.jpg", result.getItems().get(0).getImageUrl());
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Get menu: It should not modify the URL if it starts with 'http'")
    void getMenu_ShouldNotModifyUrl_WhenItAlreadyStartsHttp() {
        Page<Dishes> mockPage = new PageImpl<>(List.of(new Dishes()));
        DishListResponse dishResponse = DishListResponse.builder().imageUrl("https://external.com/img.jpg").build();

        when(_dishRepo.findAllDishes(any(), any())).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(any(), anyString())).thenReturn(dishResponse);

        PagedResult<DishListResponse> result = _dishServices.getMenu(new DishFilterRequest(), new PaggedRequest());

        assertEquals("https://external.com/img.jpg", result.getItems().get(0).getImageUrl());
    }

    @Test
    @DisplayName("Get Menu: It should handle a blank results page correctly")
    void getMenu_ShouldHandleEmptyPage() {
        when(_dishRepo.findAllDishes(any(), any())).thenReturn(Page.empty());

        PagedResult<DishListResponse> result = _dishServices.getMenu(new DishFilterRequest(), new PaggedRequest());

        assertTrue(result.getItems().isEmpty());
    }

    @Test
    @DisplayName("remove: should disable the availability of the dish, add the reason and date of deletion (soft delete)")
    void remove_ShouldMarkDishAsDeleted_AndSaveToRepository() {
        String token = "DISH_TOKEN_123";
        Dishes dish = new Dishes();
        dish.setToken(token);
        dish.setAvailable(true);
        dish.setUnavailableReason(null);
        dish.setDeletedAt(null);

        when(_dishRepo.findByToken(token)).thenReturn(dish);

        _dishServices.remove(token);

        assertNull(dish.getImageUrl());
        verify(_s3Services, times(1)).deleteFile(any());

        assertFalse(dish.isAvailable());
        assertEquals("Dish is deleted", dish.getUnavailableReason());
        assertNotNull(dish.getDeletedAt());

        verify(_dishRepo, times(1)).findByToken(token);
        verify(_dishRepo, times(1)).save(dish);
    }

    @Test
    @DisplayName("changeAvailable: should restore the availability of the dish and remove the reason for its unavailability")
    void changeAvailable_ShouldSetAvailable_AndClearReason() {
        Dishes dish = new Dishes();
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
    }

    @Test
    @DisplayName("changeAvailable: should disable accessibility and set custom reason ")
    void changeAvailable_ShouldSetUnavailable_AndSetCustomReason() {
        Dishes dish = new Dishes();
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
    }

    @Test
    @DisplayName("changeAvailable: should disable accessibility and set default reason when null/empty string is sent")
    void changeAvailable_ShouldSetUnavailable_AndSetDefaultReason_WhenReasonIsNullOrBlank() {
        Dishes dish = new Dishes();
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
    }

    @Test
    @DisplayName("edit: Should update basic dish properties (name, price) and save without interacting with S3")
    void edit_ShouldUpdateBasicProperties_AndSave() {
        EditDishRequest request = new EditDishRequest();
        request.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        request.setNewName("  New Steak Name  ");
        request.setPrice(1500);

        Dishes dish = new Dishes();
        dish.setName("Old Name");
        dish.setPrice(1000);

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);

        _dishServices.edit(request);

        assertEquals("New Steak Name", dish.getName(), "Name should be updated and trimmed");
        assertEquals(1500, dish.getPrice(), "Price should be updated");
        verify(_dishRepo, times(1)).save(dish);
        verifyNoInteractions(_s3Services);
        verifyNoInteractions(_ingredientsRepo);
    }

    @Test
    @DisplayName("edit: Should update category and ingredients successfully")
    void edit_ShouldUpdateCategoryAndIngredients() {
        EditDishRequest request = new EditDishRequest();
        request.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        request.setCategoryToken("CAT_MAIN");
        request.setIngredientTokens(List.of("ING_BEEF"));

        Dishes dish = new Dishes();
        DishesCategories category = new DishesCategories();
        Ingredients ingredient = new Ingredients();

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);
        when(_dishRepo.findCategoryByToken("CAT_MAIN")).thenReturn(category);
        when(_ingredientsRepo.findByToken("ING_BEEF")).thenReturn(ingredient);

        _dishServices.edit(request);

        assertEquals(category, dish.getCategory());
        assertEquals(1, dish.getIngredients().size());
        assertTrue(dish.getIngredients().contains(ingredient));
        verify(_dishRepo, times(1)).save(dish);
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
        dish.setImageUrl("old_steak_image.jpg");

        when(_dishRepo.findByToken(TestConstants.FAKE_DISH_TOKEN)).thenReturn(dish);
        when(_s3Services.generateUniqFileName("steak.png")).thenReturn("new_uuid_steak.png");
        when(_s3Services.uploadFromStream(mockInputStream, "new_uuid_steak.png", "image/png", 1024L))
                .thenReturn("new_uuid_steak.png");

        _dishServices.edit(request);

        verify(_s3Services, times(1)).deleteFile("old_steak_image.jpg");
        assertEquals("new_uuid_steak.png", dish.getImageUrl(), "Image URL should be updated");
        verify(_dishRepo, times(1)).save(dish);
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
}
