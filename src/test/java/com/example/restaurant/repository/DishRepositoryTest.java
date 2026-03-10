package com.example.restaurant.repository;


import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
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

        Page<Dishes> mockPage = new PageImpl<>(List.of(dishes));

        when(_jpaDishRepo.findAll(expectedPageable)).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(any(Dishes.class), eq("pl"))).thenReturn(dishResponse);

        List<DishListResponse> result = _dishRepo.findAllDishes("pl", filterRequest, paggedRequest);

        assertEquals(1, result.size());
        assertEquals("Pizza", result.get(0).getName());

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

        Page<Dishes> mockPage = new PageImpl<>(List.of(dishes));

        when(_jpaDishRepo.findWithoutAllergens(List.of("GLUTEN"), expectedPageable)).thenReturn(mockPage);
        when(_dishMapper.toDishListResponse(any(Dishes.class), eq("pl"))).thenReturn(dishList);

        List<DishListResponse> result = _dishRepo.findAllDishes("pl", filter, pagged);

        assertEquals(1, result.size());
        assertEquals("Salad", result.get(0).getName());
        verify(_jpaDishRepo, times(1)).findWithoutAllergens(List.of("GLUTEN"), expectedPageable);

        verify(_jpaDishRepo, never()).findAll(any(Pageable.class));
        verify(_dishMapper, times(1)).toDishListResponse(any(Dishes.class), eq("pl"));
    }
}
