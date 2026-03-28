package com.example.restaurant.repository;


import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.repository.interfaces.jpa.IJpaDishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DishRepositoryTest {
    @Mock
    private IJpaDishRepository _jpaDishRepo;

    @Mock
    private DishMapper _dishMapper;

    @InjectMocks
    private DishRepository _dishRepo;

    @Test
    void findAllDishes_ShouldCallJpaFindAll_WhenNoAllergensExcluded() {
        Dishes dishes = new Dishes();
        dishes.setName("Pizza");

        DishListResponse dishResponse = DishListResponse
                .builder()
                .name("Pizza")
                .build();

        DishFilterRequest filterRequest = new DishFilterRequest();
        PaggedRequest paggedRequest = new PaggedRequest();

        Pageable expectedPageable = PageRequest.of(0, 10);

        Page<Dishes> mockPage = new PageImpl<>(List.of(dishes), expectedPageable, 1);

        when(_jpaDishRepo.findAll(expectedPageable)).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(any(Dishes.class), eq("pl"))).thenReturn(dishResponse);

        PagedResult<DishListResponse> result = _dishRepo.findAllDishes("pl", filterRequest, paggedRequest);

        assertEquals(1, result.getItems().size());
        assertEquals("Pizza", result.getItems().get(0).getName());
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getTotalPages());

        verify(_jpaDishRepo, times(1)).findAll(expectedPageable);
        verify(_dishMapper, times(1)).toDishListResponse(any(Dishes.class), eq("pl"));
    }

    @Test
    void findAllDishes_ShouldCallJpaFindWithoutAllergens_WhenAllergensAreExcluded() {
        Dishes dishes = new Dishes();
        dishes.setName("Salad");

        DishListResponse dishList = DishListResponse
                .builder()
                .name("Salad")
                .build();

        DishFilterRequest filter = new DishFilterRequest();
        filter.setExcludedAllergens(List.of("GLUTEN"));

        PaggedRequest pagged = new PaggedRequest();
        pagged.setPage(2);
        pagged.setSize(5);

        Pageable expectedPageable = PageRequest.of(1, 5);

        Page<Dishes> mockPage = new PageImpl<>(List.of(dishes), expectedPageable, 10);

        when(_jpaDishRepo.findWithoutAllergens(List.of("GLUTEN"), expectedPageable)).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(any(Dishes.class), eq("pl"))).thenReturn(dishList);

        PagedResult<DishListResponse> result = _dishRepo.findAllDishes("pl", filter, pagged);

        assertEquals(1, result.getItems().size());
        assertEquals("Salad", result.getItems().get(0).getName());
        assertEquals(2, result.getPageNumber());
        assertEquals(5, result.getPageSize());

        verify(_jpaDishRepo, times(1)).findWithoutAllergens(List.of("GLUTEN"), expectedPageable);
        verify(_jpaDishRepo, never()).findAll(any(Pageable.class));
        verify(_dishMapper, times(1)).toDishListResponse(any(Dishes.class), eq("pl"));
    }

    @Test
    void findAllDishes_ShouldAppendS3Url_WhenDishHasImageUrl() {
        ReflectionTestUtils.setField(_dishRepo, "s3Endpoint", "http://localhost:9000 ");
        ReflectionTestUtils.setField(_dishRepo, "s3BucketName", "restaurant-images");

        Dishes dishes = new Dishes();

        DishListResponse dishResponse = DishListResponse.builder()
                .name("Steak")
                .imageUrl("steak.jpg")
                .build();

        DishFilterRequest filterRequest = new DishFilterRequest();
        PaggedRequest paggedRequest = new PaggedRequest();
        Pageable expectedPageable = PageRequest.of(0, 10);
        Page<Dishes> mockPage = new PageImpl<>(List.of(dishes), expectedPageable, 1);

        when(_jpaDishRepo.findAll(expectedPageable)).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(any(Dishes.class), eq("pl"))).thenReturn(dishResponse);

        PagedResult<DishListResponse> result = _dishRepo.findAllDishes("pl", filterRequest, paggedRequest);

        assertEquals(1, result.getItems().size());
        assertEquals("http://localhost:9000/restaurant-images/steak.jpg", result.getItems().get(0).getImageUrl());
    }

    @Test
    void listForOrder_ShouldReturnListOfDishes() {
        List<String> tokens = List.of("TOKEN1", "TOKEN2");
        List<Dishes> expectedDishes = List.of(new Dishes(), new Dishes());

        when(_jpaDishRepo.findAllByTokenIn(tokens)).thenReturn(expectedDishes);

        List<Dishes> result = _dishRepo.listForOrder(tokens);

        assertEquals(2, result.size());
        verify(_jpaDishRepo, times(1)).findAllByTokenIn(tokens);
    }
}
