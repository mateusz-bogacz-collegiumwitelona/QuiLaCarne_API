package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.exceptions.StatusNotFoundException;
import com.example.restaurant.exceptions.TableNotFoundException;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
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
    void getTables_ShouldReturnAvailableStatus_WhenDatesProvidedAndLangIsEnglish() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        TableFilterRequest request = new TableFilterRequest();
        request.setStartTime(OffsetDateTime.now().plusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(2));

        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setToken(TestConstants.FAKE_TABLE_TOKEN);
        mockTable.setTableNumber(1);
        mockTable.setCapacity(4);

        when(_tableRepo.findAllTables(request.getStartTime(), request.getEndTime()))
                .thenReturn(List.of(mockTable));

        ResultHandler<List<TableListResponse>> result = _tableServices.getTables(request);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals(1, result.getData().size());
        assertEquals("Available", result.getData().get(0).getStatus());

        verify(_tableRepo).findAllTables(request.getStartTime(), request.getEndTime());
    }

    @Test
    void getTables_ShouldReturnWolnyStatus_WhenDatesProvidedAndLangIsPolish() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));

        TableFilterRequest request = new TableFilterRequest();
        request.setStartTime(OffsetDateTime.now().plusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(2));

        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setToken(TestConstants.FAKE_TABLE_TOKEN);

        when(_tableRepo.findAllTables(request.getStartTime(), request.getEndTime()))
                .thenReturn(List.of(mockTable));

        ResultHandler<List<TableListResponse>> result = _tableServices.getTables(request);

        assertTrue(result.isSuccess());
        assertEquals("Wolny", result.getData().get(0).getStatus());
    }

    @Test
    void getTables_ShouldReturnTranslatedStatus_WhenNoDatesAndLangIsPolish() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));

        TableFilterRequest request = new TableFilterRequest();

        TableStatus mockStatus = mock(TableStatus.class);
        when(mockStatus.getNamePl()).thenReturn("Zajęty");

        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setToken(TestConstants.FAKE_TABLE_TOKEN);
        mockTable.setTableStatus(Set.of(mockStatus));

        when(_tableRepo.findAllTables(null, null)).thenReturn(List.of(mockTable));

        ResultHandler<List<TableListResponse>> result = _tableServices.getTables(request);

        assertTrue(result.isSuccess());
        assertEquals("Zajęty", result.getData().get(0).getStatus());
        verify(mockStatus).getNamePl();
    }

    @Test
    void getTables_ShouldReturnTranslatedStatus_WhenNoDatesAndLangIsEnglish() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        TableFilterRequest request = new TableFilterRequest();

        TableStatus mockStatus = mock(TableStatus.class);
        when(mockStatus.getNameEn()).thenReturn("Occupied");

        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setToken(TestConstants.FAKE_TABLE_TOKEN);
        mockTable.setTableStatus(Set.of(mockStatus));

        when(_tableRepo.findAllTables(null, null)).thenReturn(List.of(mockTable));

        ResultHandler<List<TableListResponse>> result = _tableServices.getTables(request);

        assertTrue(result.isSuccess());
        assertEquals("Occupied", result.getData().get(0).getStatus());
        verify(mockStatus).getNameEn();
    }

    @Test
    void getTables_ShouldReturnUnknown_WhenTableHasNoStatus() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        TableFilterRequest request = new TableFilterRequest();

        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setTableStatus(Set.of());

        when(_tableRepo.findAllTables(null, null)).thenReturn(List.of(mockTable));

        ResultHandler<List<TableListResponse>> result = _tableServices.getTables(request);

        assertTrue(result.isSuccess());
        assertEquals("UNKNOWN", result.getData().get(0).getStatus());
    }

    @Test
    void getTables_ShouldReturnStatusInEnglish_WhenLanguageIsGerman() {
        LocaleContextHolder.setLocale(Locale.GERMAN);

        TableStatus mockStatus = new TableStatus();
        mockStatus.setNameEn("Occupied");
        RestaurantTables table = new RestaurantTables();
        table.setTableStatus(Set.of(mockStatus));

        when(_tableRepo.findAllTables(null, null)).thenReturn(List.of(table));

        var result = _tableServices.getTables(new TableFilterRequest());

        assertEquals("Occupied", result.getData().get(0).getStatus());
    }

    @Test
    void changeStatusToClean_ShouldReturnSuccess_WhenRepoSucceeds() {
        RestaurantTables mockTable = new RestaurantTables();
        TableStatus mockStatus = new TableStatus();

        when(_tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(mockTable);
        when(_tableRepo.findStatusByToken("CLEANING")).thenReturn(mockStatus);

        ResultHandler<Void> result = _tableServices.changeStatusToClean(TestConstants.FAKE_TABLE_TOKEN);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Status change successfully", result.getMessage());

        verify(_tableRepo).findByToken(TestConstants.FAKE_TABLE_TOKEN);
        verify(_tableRepo).findStatusByToken("CLEANING");
        verify(_tableRepo).save(mockTable);
    }

    @Test
    void changeStatusToClean_ShouldThrowTableNotFoundException_WhenTableNotFound() {
        when(_tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenThrow(new TableNotFoundException("Table not found"));

        assertThrows(TableNotFoundException.class,
                () -> _tableServices.changeStatusToClean(TestConstants.FAKE_TABLE_TOKEN)
        );

        verify(_tableRepo, never()).save(any());
    }

    @Test
    void changeStatusToClean_ShouldThrowTableStatusNotFoundException_WhenStatusNotFound() {
        RestaurantTables mockTable = new RestaurantTables();
        when(_tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(mockTable);
        when(_tableRepo.findStatusByToken("CLEANING"))
                .thenThrow(new StatusNotFoundException("Table status not found"));

        assertThrows(StatusNotFoundException.class,
                () -> _tableServices.changeStatusToClean(TestConstants.FAKE_TABLE_TOKEN)
        );

        verify(_tableRepo, never()).save(any());
    }
}