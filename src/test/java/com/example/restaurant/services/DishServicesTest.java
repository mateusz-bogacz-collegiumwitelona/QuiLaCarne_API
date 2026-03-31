package com.example.restaurant.services;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.repository.interfaces.IDishRepository;
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

        assertFalse(dish.isAvailable());
        assertEquals("Dish is deleted", dish.getUnavailableReason());
        assertNotNull(dish.getDeletedAt());

        verify(_dishRepo, times(1)).findByToken(token);
        verify(_dishRepo, times(1)).save(dish);
    }
}
