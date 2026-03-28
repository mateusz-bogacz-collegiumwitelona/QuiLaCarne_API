package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.exceptions.TableNotFoundException;
import com.example.restaurant.exceptions.TableStatusNotFoundException;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TableRepositoryTest {
    @Mock
    private IJpaTableRepository _jpaTableRepo;

    @Mock
    private IJpaTableStatusRepository _jpaTableStatusRepo;

    @InjectMocks
    private TableRespository _tableRepo;

    @Test
    void findAllTables_ShouldMapEntityToDto_WithTranslation() {
        TableStatus mockStatus = mock(TableStatus.class);
        when(mockStatus.getNamePl()).thenReturn("Wolny");

        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setTableNumber(100);
        mockTable.setCapacity(4);
        mockTable.setToken(TestConstants.FAKE_USER_TOKEN);
        mockTable.setTableStatus(Set.of(mockStatus));

        when(_jpaTableRepo.findAll()).thenReturn(List.of(mockTable));

        var result = _tableRepo.findAllTables("pl", null, null);

        assertEquals(1, result.size());
        var dto = result.get(0);
        assertEquals(TestConstants.FAKE_USER_TOKEN, dto.getToken());
        assertEquals(100, dto.getTableNumber());
        assertEquals("Wolny", dto.getStatus());

        verify(mockStatus).getNamePl();
    }

    @Test
    void findAllTables_ShouldReturnUnknown_WhenStatusIsEmpty() {
        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setTableStatus(Set.of());

        when(_jpaTableRepo.findAll()).thenReturn(List.of(mockTable));

        var result = _tableRepo.findAllTables("pl", null, null);

        assertEquals("UNKNOWN", result.get(0).getStatus());
    }

    @Test
    void findAllTables_ShouldReturnAvailableStatus_WhenCheckingAvailability() {
        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setTableNumber(101);
        mockTable.setCapacity(4);
        mockTable.setToken(TestConstants.FAKE_USER_TOKEN);
        mockTable.setTableStatus(Set.of());

        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime endTime = startTime.plusHours(2);

        when(_jpaTableRepo.findAvailableTablesInTimeframe(startTime, endTime)).thenReturn(List.of(mockTable));

        var result = _tableRepo.findAllTables("pl", startTime, endTime);

        assertEquals(1, result.size());
        assertEquals("Wolny", result.get(0).getStatus());

        verify(_jpaTableRepo).findAvailableTablesInTimeframe(startTime, endTime);
        verify(_jpaTableRepo, never()).findAll();
    }

    @Test
    void changeStatus_ShouldUpdateStatusAndSave_WhenValid() {
        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setToken(TestConstants.FAKE_TABLE_TOKEN);

        TableStatus mockStatus = new TableStatus();
        mockStatus.setToken("CLEANING");

        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(Optional.of(mockTable));
        when(_jpaTableStatusRepo.findByToken("CLEANING")).thenReturn(Optional.of(mockStatus));

        _tableRepo.changeStatus(TestConstants.FAKE_TABLE_TOKEN, "CLEANING");

        assertNotNull(mockTable.getTableStatus());
        assertTrue(mockTable.getTableStatus().contains(mockStatus));
        verify(_jpaTableRepo, times(1)).save(mockTable);
    }

    @Test
    void changeStatus_ShouldThrowTableNotFoundException_WhenTableDoesNotExist() {
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(Optional.empty());

        TableNotFoundException exception = assertThrows(TableNotFoundException.class, () ->
                _tableRepo.changeStatus(TestConstants.FAKE_TABLE_TOKEN, "CLEANING")
        );

        assertEquals("Table not found", exception.getMessage());
        verify(_jpaTableStatusRepo, never()).findByToken(anyString());
        verify(_jpaTableRepo, never()).save(any());
    }

    @Test
    void changeStatus_ShouldThrowTableStatusNotFoundException_WhenStatusDoesNotExist() {
        RestaurantTables mockTable = new RestaurantTables();

        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(Optional.of(mockTable));
        when(_jpaTableStatusRepo.findByToken("CLEANING")).thenReturn(Optional.empty());

        TableStatusNotFoundException exception = assertThrows(TableStatusNotFoundException.class, () ->
                _tableRepo.changeStatus(TestConstants.FAKE_TABLE_TOKEN, "CLEANING")
        );

        assertEquals("Table status not found", exception.getMessage());
        verify(_jpaTableRepo, never()).save(any());
    }

    @Test
    void findByToken_ShouldReturnTable_WhenFound() {
        RestaurantTables mockTable = new RestaurantTables();
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.of(mockTable));

        RestaurantTables result = _tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN);

        assertNotNull(result);
        verify(_jpaTableRepo, times(1)).findByToken(TestConstants.FAKE_TABLE_TOKEN);
    }

    @Test
    void findByToken_ShouldThrowException_WhenTableNotFound() {
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.empty());

        TableNotFoundException exception = assertThrows(TableNotFoundException.class, () ->
                _tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)
        );

        assertEquals("Table not found", exception.getMessage());
    }
}
