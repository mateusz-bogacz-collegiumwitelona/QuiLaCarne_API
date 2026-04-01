package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.ITableRespository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableServices Unit Tests")
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
    @DisplayName("Get Tables: Success - Should return 'Available' status when dates are provided and lang is English")
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

        List<TableListResponse> result = _tableServices.getTables(request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Available", result.get(0).getStatus());
        verify(_tableRepo).findAllTables(request.getStartTime(), request.getEndTime());
    }

    @Test
    @DisplayName("Get Tables: Success - Should return 'Wolny' status when dates are provided and lang is Polish")
    void getTables_ShouldReturnWolnyStatus_WhenDatesProvidedAndLangIsPolish() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));
        TableFilterRequest request = new TableFilterRequest();
        request.setStartTime(OffsetDateTime.now().plusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(2));

        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setToken(TestConstants.FAKE_TABLE_TOKEN);

        when(_tableRepo.findAllTables(request.getStartTime(), request.getEndTime()))
                .thenReturn(List.of(mockTable));

        List<TableListResponse> result = _tableServices.getTables(request);

        assertEquals("Wolny", result.get(0).getStatus());
    }

    @Test
    @DisplayName("Get Tables: Failure - Should throw IllegalStateException when start time is after end time")
    void getTables_ShouldThrowException_WhenDatesAreInvalid() {
        TableFilterRequest request = new TableFilterRequest();
        request.setStartTime(OffsetDateTime.now().plusHours(2));
        request.setEndTime(OffsetDateTime.now().plusHours(1));

        assertThrows(IllegalStateException.class, () -> _tableServices.getTables(request));
    }

    @Test
    @DisplayName("Get Tables: Success - Should return translated status from DB when no dates are provided")
    void getTables_ShouldReturnTranslatedStatus_FromDb() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        TableFilterRequest request = new TableFilterRequest();

        TableStatus mockStatus = new TableStatus();
        mockStatus.setNameEn("Occupied");
        mockStatus.setNamePl("Zajęty");

        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setToken(TestConstants.FAKE_TABLE_TOKEN);
        mockTable.setTableStatus(Set.of(mockStatus));

        when(_tableRepo.findAllTables(null, null)).thenReturn(List.of(mockTable));

        List<TableListResponse> result = _tableServices.getTables(request);

        assertEquals("Occupied", result.get(0).getStatus());
    }

    @Test
    @DisplayName("Get Tables: Success - Should return empty list when no tables are found")
    void getTables_ShouldReturnEmptyList_WhenNoResults() {
        when(_tableRepo.findAllTables(any(), any())).thenReturn(List.of());

        List<TableListResponse> result = _tableServices.getTables(new TableFilterRequest());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Change Status: Success - Should update table status to CLEANING")
    void changeStatusToClean_Successful() {
        RestaurantTables mockTable = new RestaurantTables();
        TableStatus mockStatus = new TableStatus();

        when(_tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(mockTable);
        when(_tableRepo.findStatusByToken("CLEANING")).thenReturn(mockStatus);

        assertDoesNotThrow(() -> _tableServices.changeStatusToClean(TestConstants.FAKE_TABLE_TOKEN));

        // Assert
        verify(_tableRepo).save(mockTable);
        assertTrue(mockTable.getTableStatus().contains(mockStatus));
    }

    @Test
    @DisplayName("Change Status: Failure - Should throw EntityNotFoundException when table is missing")
    void changeStatusToClean_ShouldThrowEntityNotFound_WhenTableMissing() {
        when(_tableRepo.findByToken(anyString())).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> _tableServices.changeStatusToClean(TestConstants.FAKE_TABLE_TOKEN));
        verify(_tableRepo, never()).save(any());
    }

    @Test
    @DisplayName("Change Status: Failure - Should throw EntityNotFoundException when status token is missing in DB")
    void changeStatusToClean_ShouldThrowEntityNotFound_WhenStatusMissing() {
        when(_tableRepo.findByToken(anyString())).thenReturn(new RestaurantTables());
        when(_tableRepo.findStatusByToken("CLEANING")).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> _tableServices.changeStatusToClean(TestConstants.FAKE_TABLE_TOKEN));
    }

    @Test
    @DisplayName("Change Status To Out Of Service: Success when table and status exist")
    void changeStatusToOutOfService_ShouldChangeStatus_WhenValid() {
        String tableToken = "table-123";
        RestaurantTables table = new RestaurantTables();

        TableStatus outOfServiceStatus = new TableStatus();
        outOfServiceStatus.setToken("OUT_OF_SERVICE");
        outOfServiceStatus.setNameEn("Out of service");

        when(_tableRepo.findByToken(tableToken)).thenReturn(table);
        when(_tableRepo.findStatusByToken("OUT_OF_SERVICE")).thenReturn(outOfServiceStatus);

        _tableServices.changeStatusToOutOfService(tableToken);

        assertTrue(table.getTableStatus().contains(outOfServiceStatus));
        verify(_tableRepo, times(1)).save(table);
    }

    @Test
    @DisplayName("Change Status To Out Of Service: Throws Exception when table is not found")
    void changeStatusToOutOfService_ShouldThrowException_WhenTableNotFound() {
        String tableToken = "invalid-token";

        when(_tableRepo.findByToken(tableToken)).thenReturn(null);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> _tableServices.changeStatusToOutOfService(tableToken));

        assertEquals("Table with token " + tableToken + " not found", exception.getMessage());
        verify(_tableRepo, never()).save(any());
    }

    @Test
    @DisplayName("Change Status To Out Of Service: Throws Exception when status is not found in DB")
    void changeStatusToOutOfService_ShouldThrowException_WhenStatusNotFound() {
        String tableToken = "table-123";
        RestaurantTables table = new RestaurantTables();

        when(_tableRepo.findByToken(tableToken)).thenReturn(table);
        when(_tableRepo.findStatusByToken("OUT_OF_SERVICE")).thenReturn(null); // Brak statusu w bazie

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> _tableServices.changeStatusToOutOfService(tableToken));

        assertEquals("Table status 'OUT_OF_SERVICE' not found", exception.getMessage());
        verify(_tableRepo, never()).save(any());
    }
}