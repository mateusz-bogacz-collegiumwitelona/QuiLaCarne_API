package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.ITableRespository;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TableServicesTest {
    @Mock
    private ITableRespository _tableRepo;

    @InjectMocks
    private TableServices _tableServices;

    @Test
    public void getTables_ShouldReturnSuccess_WithCorrectLanguage() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        List<TableListResponse> mockData = List.of(
                TableListResponse.builder()
                        .token(TestConstants.FAKE_TOKEN)
                        .status("Available")
                        .build()
        );

        when(_tableRepo.findAllTables("en")).thenReturn(mockData);

        ResultHandler<List<TableListResponse>> result = _tableServices.getTables();

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals(1, result.getData().size());
        assertEquals("Available", result.getData().get(0).getStatus());

        verify(_tableRepo, times(1)).findAllTables("en");

        LocaleContextHolder.resetLocaleContext();
    }

}
