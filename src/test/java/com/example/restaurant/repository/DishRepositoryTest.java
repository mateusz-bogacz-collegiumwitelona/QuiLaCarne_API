package com.example.restaurant.repository;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DishRepositoryTest {
    @Mock
    private IJpaDishRepository _jpaDishRepo;

    @InjectMocks
    private DishRepository _dishRepo;

    @Test
    void findAllDishes_ShouldCallJpaFindAll_WhenNoAllergensExcluded() {
        Dishes dishes = new Dishes();
        dishes.setName("Pizza");

        DishFilterRequest filterRequest = new DishFilterRequest();
        PaggedRequest paggedRequest = new PaggedRequest();
        paggedRequest.setPage(1);
        paggedRequest.setSize(10);

        Pageable expectedPageable = PageRequest.of(0, 10);
        Page<Dishes> mockPage = new PageImpl<>(List.of(dishes), expectedPageable, 1);

        when(_jpaDishRepo.findAll(expectedPageable)).thenReturn(mockPage);

        Page<Dishes> result = _dishRepo.findAllDishes(filterRequest, paggedRequest);

        assertEquals(1, result.getContent().size());
        assertEquals("Pizza", result.getContent().get(0).getName());

        verify(_jpaDishRepo, times(1)).findAll(expectedPageable);
    }

    @Test
    void findAllDishes_ShouldCallJpaFindWithoutAllergens_WhenAllergensAreExcluded() {
        Dishes dishes = new Dishes();
        dishes.setName("Salad");

        DishFilterRequest filter = new DishFilterRequest();
        filter.setExcludedAllergens(List.of("GLUTEN"));

        PaggedRequest pagged = new PaggedRequest();
        pagged.setPage(2);
        pagged.setSize(5);

        Pageable expectedPageable = PageRequest.of(1, 5);
        Page<Dishes> mockPage = new PageImpl<>(List.of(dishes), expectedPageable, 10);

        when(_jpaDishRepo.findWithoutAllergens(List.of("GLUTEN"), expectedPageable)).thenReturn(mockPage);

        Page<Dishes> result = _dishRepo.findAllDishes(filter, pagged);

        assertEquals(1, result.getContent().size());
        assertEquals("Salad", result.getContent().get(0).getName());

        verify(_jpaDishRepo, times(1)).findWithoutAllergens(List.of("GLUTEN"), expectedPageable);
        verify(_jpaDishRepo, never()).findAll(any(Pageable.class));
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

    @Test
    void get_ShouldReturnDish_WhenTokenExists() {
        Dishes d1 = new Dishes();
        d1.setToken("T1");
        Dishes d2 = new Dishes();
        d2.setToken("T2");

        Dishes result = _dishRepo.get(List.of(d1, d2), "T2");

        assertEquals("T2", result.getToken());
    }

    @Test
    void get_ShouldThrowException_WhenTokenDoesNotExist() {
        assertThrows(RuntimeException.class, () -> _dishRepo.get(List.of(), "MISSING"));
    }

    @Test
    void findByIngredientsId_ShouldReturnListOfDishes() {
        UUID id = UUID.randomUUID();
        List<Dishes> expectedDishes = List.of(new Dishes(), new Dishes());
        when(_jpaDishRepo.findByIngredientsId(id)).thenReturn(expectedDishes);

        List<Dishes> result = _dishRepo.findByIngredientsId(id);

        assertEquals(2, result.size());
        verify(_jpaDishRepo).findByIngredientsId(id);
    }
}