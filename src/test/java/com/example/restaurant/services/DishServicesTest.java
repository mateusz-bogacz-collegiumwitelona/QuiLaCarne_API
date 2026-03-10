package com.example.restaurant.services;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IDishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;

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

    @InjectMocks
    private DishServices _dishServices;

    @Test
    void getMenu_ShouldUseCurrnetLocale_AndReturnSuccess() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        DishFilterRequest request = new DishFilterRequest();
        PaggedRequest pagged = new PaggedRequest();

        List<DishListResponse> mockData = List.of(DishListResponse
                .builder()
                .name("Steak")
                .build()
        );

        when(_dishRepo.findAllDishes("en", request, pagged)).thenReturn(mockData);

        ResultHandler<List<DishListResponse>> result = _dishServices.getMenu(request, pagged);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals(1, result.getData().size());
        assertEquals("Steak", result.getData().get(0).getName());
        verify(_dishRepo).findAllDishes("en", request, pagged);
        LocaleContextHolder.resetLocaleContext();

    }
}
