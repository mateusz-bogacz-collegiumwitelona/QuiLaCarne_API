package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.AddTableRequest;
import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
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

    @Mock
    private NotificationServices _notification;

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
        verify(_notification, times(1)).sendToTopic(eq("tables"), anyString());
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
        verify(_notification, times(1)).sendToTopic(eq("tables"), anyString());
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

    @Test
    @DisplayName("Add Table: Success when valid request")
    void add_ShouldSaveTable_WhenValidRequest() {
        AddTableRequest request = new AddTableRequest();
        request.setTableNumber(5);
        request.setCapacity(4);

        TableStatus availableStatus = new TableStatus();
        availableStatus.setToken("AVAILABLE");

        when(_tableRepo.existsByTableNumber(5)).thenReturn(false);
        when(_tableRepo.findStatusByToken("AVAILABLE")).thenReturn(availableStatus);

        _tableServices.add(request);

        verify(_tableRepo, times(1)).save(argThat(table ->
                table.getTableNumber() == 5 &&
                        table.getCapacity() == 4 &&
                        table.getTableStatus().contains(availableStatus)
        ));
        verify(_notification, times(1)).sendToTopic(eq("tables/layout"), anyString());
    }

    @Test
    @DisplayName("Add Table: Throws EntityAlreadyExistsException when table number exists")
    void add_ShouldThrowException_WhenTableNumberExists() {
        AddTableRequest request = new AddTableRequest();
        request.setTableNumber(5);
        request.setCapacity(4);

        when(_tableRepo.existsByTableNumber(5)).thenReturn(true);

        EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class,
                () -> _tableServices.add(request));

        assertEquals("Table with number 5 already exists", exception.getMessage());
        verify(_tableRepo, never()).save(any());
    }

    @Test
    @DisplayName("Add Table: Throws EntityNotFoundException when AVAILABLE status is missing")
    void add_ShouldThrowException_WhenStatusNotFound() {

        AddTableRequest request = new AddTableRequest();
        request.setTableNumber(5);
        request.setCapacity(4);

        when(_tableRepo.existsByTableNumber(5)).thenReturn(false);
        when(_tableRepo.findStatusByToken("AVAILABLE")).thenReturn(null);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> _tableServices.add(request));

        assertEquals("Default table status 'AVAILABLE' not found in database", exception.getMessage());
        verify(_tableRepo, never()).save(any());
    }

    @Test
    @DisplayName("Delete Table: Success with Soft Delete")
    void delete_ShouldSetDeletedAt_WhenTableExists() {
        String tableToken = "table-123";
        RestaurantTables table = new RestaurantTables();

        when(_tableRepo.findByToken(tableToken)).thenReturn(table);

        _tableServices.delete(tableToken);

        assertNotNull(table.getDeletedAt());
        verify(_tableRepo, times(1)).save(table);
        verify(_notification, times(1)).sendToTopic(eq("tables/layout"), anyString());
    }

    @Test
    @DisplayName("Delete Table: Throws Exception when table not found")
    void delete_ShouldThrowException_WhenTableNotFound() {
        String tableToken = "invalid-token";

        when(_tableRepo.findByToken(tableToken))
                .thenThrow(new EntityNotFoundException("Table not found"));

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> _tableServices.delete(tableToken));

        assertEquals("Table not found", exception.getMessage());
        verify(_tableRepo, never()).save(any());
    }


    @Test
    @DisplayName("getDictionary: Returns empty list when repository returns empty")
    void getDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
        when(_tableRepo.findAllStatuses()).thenReturn(new java.util.ArrayList<>());
        List<EntityResponse> result = _tableServices.getDictionary();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getDictionary: Returns Polish names when language is pl")
    void getDictionary_ShouldReturnPolishNames_WhenLanguageIsPl() {
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_PL));
        TableStatus status = new TableStatus();
        status.setToken(TestConstants.STATUS_AVAILABLE);
        status.setNamePl("Wolny PL");
        status.setNameEn("Available EN");

        when(_tableRepo.findAllStatuses()).thenReturn(List.of(status));

        List<EntityResponse> result = _tableServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals(TestConstants.STATUS_AVAILABLE, result.get(0).getToken());
        assertEquals("Wolny PL", result.get(0).getName());
    }

    @Test
    @DisplayName("getDictionary: Returns English names when language is not pl")
    void getDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_EN));
        TableStatus status = new TableStatus();
        status.setToken(TestConstants.STATUS_CLEANING);
        status.setNamePl("Sprzątanie PL");
        status.setNameEn("Cleaning EN");

        when(_tableRepo.findAllStatuses()).thenReturn(List.of(status));

        List<EntityResponse> result = _tableServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals(TestConstants.STATUS_CLEANING, result.get(0).getToken());
        assertEquals("Cleaning EN", result.get(0).getName());
    }

    @Test
    @DisplayName("addStatus: Should save table status when data is correct")
    void addStatus_ShouldSaveTableStatus_WhenDataIsCorrect() {
        AddEntityRequest request = new AddEntityRequest();
        request.setNamePl("Nowy Status Stolika PL");
        request.setNameEn("New Table Status EN");

        when(_tableRepo.isStatusNameTaken(anyString(), anyString())).thenReturn(false);

        assertDoesNotThrow(() -> _tableServices.addStatus(request));
        verify(_tableRepo, times(1)).saveStatus(argThat(status ->
                status.getNamePl().equals("Nowy Status Stolika PL") &&
                        status.getNameEn().equals("New Table Status EN") &&
                        status.getToken().equals("NEW_TABLE_STATUS_EN")
        ));
    }

    @Test
    @DisplayName("removeStatus: Should soft delete table status and reassign tables to AVAILABLE")
    void removeStatus_ShouldSoftDelete_AndReassignToAvailable() {
        String tokenToRemove = "CLEANING_TOKEN";
        TableStatus statusToRemove = new TableStatus();
        statusToRemove.setToken(tokenToRemove);

        TableStatus fallbackStatus = new TableStatus();
        fallbackStatus.setToken("AVAILABLE");

        RestaurantTables table = new RestaurantTables();
        table.getTableStatus().add(statusToRemove);

        when(_tableRepo.findStatusByToken(tokenToRemove)).thenReturn(statusToRemove);
        when(_tableRepo.findStatusByToken("AVAILABLE")).thenReturn(fallbackStatus);
        when(_tableRepo.findTablesByStatus(statusToRemove)).thenReturn(List.of(table));

        assertDoesNotThrow(() -> _tableServices.removeStatus(tokenToRemove));

        assertFalse(table.getTableStatus().contains(statusToRemove));
        assertTrue(table.getTableStatus().contains(fallbackStatus));
        verify(_tableRepo, times(1)).saveStatus(statusToRemove);
        verify(_tableRepo, times(1)).save(table);
    }
}