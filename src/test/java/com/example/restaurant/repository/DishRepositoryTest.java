package com.example.restaurant.repository;


import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.repository.interfaces.jpa.IJpaDishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void findAllDishes_ShouldCallJpaAndMapper() {
        Dishes dishes = new Dishes();
        dishes.setName("Pizza");

        DishListResponse dishResponse = DishListResponse
                .builder()
                .name("Pizza")
                .build();

        when(_jpaDishRepo.findAll()).thenReturn(List.of(dishes));
        when(_dishMapper.toDishListResponse(
                any(Dishes.class),
                eq("pl")
                )
        ).thenReturn(dishResponse);

        List<DishListResponse> result = _dishRepo.findAllDishes("pl");

        assertEquals(1, result.size());
        assertEquals(
                "Pizza",
                result.get(0).getName()
        );

        verify(_jpaDishRepo, times(1)).findAll();
        verify(_dishMapper, times(1))
                .toDishListResponse(any(Dishes.class), eq("pl"));
    }
}
