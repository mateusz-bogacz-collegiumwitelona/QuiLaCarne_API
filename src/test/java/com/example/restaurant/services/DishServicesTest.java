package com.example.restaurant.services;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.repository.interfaces.IDishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DishServicesTest {
    @Mock
    private IDishRepository _dishRepo;

    @Mock
    private DishMapper _dishMapper;
    
    @InjectMocks
    private DishServices _dishServices;

    @Test
    void getMenu_ShouldUseCurrentLocale_MapDishes_AndAppendS3Url() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ReflectionTestUtils.setField(_dishServices, "s3Endpoint", "http://localhost:9000 ");
        ReflectionTestUtils.setField(_dishServices, "s3BucketName", "restaurant-images");

        DishFilterRequest request = new DishFilterRequest();
        PaggedRequest pagged = new PaggedRequest();
        pagged.setPage(1);
        pagged.setSize(10);

        Dishes mockDish = new Dishes();
        mockDish.setName("Steak");

        Page<Dishes> mockPage = new PageImpl<>(
                List.of(mockDish),
                PageRequest.of(0, 10),
                1
        );

        DishListResponse dishResponse = DishListResponse
                .builder()
                .name("Steak")
                .imageUrl("steak.jpg")
                .build();

        when(_dishRepo.findAllDishes(request, pagged)).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(mockDish, "en")).thenReturn(dishResponse);

        ResultHandler<PagedResult<DishListResponse>> result = _dishServices.getMenu(request, pagged);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());

        assertEquals(1, result.getData().getItems().size());
        assertEquals("Steak", result.getData().getItems().get(0).getName());

        assertEquals("http://localhost:9000/restaurant-images/steak.jpg", result.getData().getItems().get(0).getImageUrl());
        assertEquals(1, result.getData().getTotalPages());

        verify(_dishRepo).findAllDishes(request, pagged);
        verify(_dishMapper).toDishListResponse(mockDish, "en");

        LocaleContextHolder.resetLocaleContext();
    }
}
