package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.ITableRespository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TableServicesTest {
    @Mock
    private ITableRespository _tableRepo;

    @InjectMocks
    private TableServices _tableServices;

    @AfterEach
    public void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    public void getTables_ShouldReturnSuccess_WithCorrectLanguage() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        TableFilterRequest request = new TableFilterRequest();
        request.setStartTime(OffsetDateTime.now().plusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(2));

        List<TableListResponse> mockData = List.of(
                TableListResponse.builder()
                        .token(TestConstants.FAKE_USER_TOKEN)
                        .status("Available")
                        .build()
        );

        when(_tableRepo.findAllTables("en", request.getStartTime(), request.getEndTime())).thenReturn(mockData);

        ResultHandler<List<TableListResponse>> result = _tableServices.getTables(request);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals(1, result.getData().size());
        assertEquals("Available", result.getData().get(0).getStatus());

        verify(_tableRepo, times(1)).findAllTables("en", request.getStartTime(), request.getEndTime());
    }


    @Test
    public void getTables_ShouldReturnSuccess_WhenValidDatesProvided() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));

        TableFilterRequest request = new TableFilterRequest();
        request.setStartTime(OffsetDateTime.now().plusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(2));

        List<TableListResponse> mockData = List.of(
                TableListResponse.builder()
                        .token(TestConstants.FAKE_USER_TOKEN)
                        .status("Wolny")
                        .build()
        );

        when(_tableRepo.findAllTables("pl", request.getStartTime(), request.getEndTime())).thenReturn(mockData);

        ResultHandler<List<TableListResponse>> result = _tableServices.getTables(request);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        verify(_tableRepo).findAllTables("pl", request.getStartTime(), request.getEndTime());
    }
}
