package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.exceptions.EntityNotFoundException;
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
    void findAllTables_ShouldCallFindAll_WhenDatesAreNull() {
        when(_jpaTableRepo.findAll()).thenReturn(List.of(new RestaurantTables()));

        List<RestaurantTables> result = _tableRepo.findAllTables(null, null);

        assertEquals(1, result.size());
        verify(_jpaTableRepo).findAll();
        verify(_jpaTableRepo, never()).findAvailableTablesInTimeframe(any(), any());
    }

    @Test
    void findAllTables_ShouldCallFindAvailableInTimeframe_WhenDatesProvided() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusHours(2);

        when(_jpaTableRepo.findAvailableTablesInTimeframe(start, end))
                .thenReturn(List.of(new RestaurantTables()));

        List<RestaurantTables> result = _tableRepo.findAllTables(start, end);

        assertEquals(1, result.size());
        verify(_jpaTableRepo).findAvailableTablesInTimeframe(start, end);
        verify(_jpaTableRepo, never()).findAll();
    }

    @Test
    void findByToken_ShouldReturnTable_WhenFound() {
        RestaurantTables mockTable = new RestaurantTables();
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.of(mockTable));

        RestaurantTables result = _tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN);

        assertNotNull(result);
        verify(_jpaTableRepo).findByToken(TestConstants.FAKE_TABLE_TOKEN);
    }

    @Test
    void findByToken_ShouldThrowTableNotFoundException_WhenNotFound() {
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                _tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)
        );

        assertEquals("Table not found", ex.getMessage());
    }

    @Test
    void isTableExist_ShouldReturnTrue_WhenTableExists() {
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.of(new RestaurantTables()));

        assertTrue(_tableRepo.isTableExist(TestConstants.FAKE_TABLE_TOKEN));
    }

    @Test
    void isTableExist_ShouldReturnFalse_WhenTableDoesNotExist() {
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.empty());

        assertFalse(_tableRepo.isTableExist(TestConstants.FAKE_TABLE_TOKEN));
    }

    @Test
    void findStatusByToken_ShouldReturnStatus_WhenFound() {
        TableStatus mockStatus = new TableStatus();
        when(_jpaTableStatusRepo.findByToken("CLEANING"))
                .thenReturn(Optional.of(mockStatus));

        TableStatus result = _tableRepo.findStatusByToken("CLEANING");

        assertNotNull(result);
        verify(_jpaTableStatusRepo).findByToken("CLEANING");
    }

    @Test
    void findStatusByToken_ShouldThrowTableStatusNotFoundException_WhenNotFound() {
        when(_jpaTableStatusRepo.findByToken("CLEANING"))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                _tableRepo.findStatusByToken("CLEANING")
        );

        assertEquals("Table status not found", ex.getMessage());
    }

    @Test
    void save_ShouldCallJpaSave() {
        RestaurantTables table = new RestaurantTables();
        _tableRepo.save(table);
        verify(_jpaTableRepo).save(table);
    }

    @Test
    void isTableAvailable_ShouldReturnTrue_WhenTableIsInAvailableList() {
        RestaurantTables table = new RestaurantTables();
        table.setToken("T1");
        OffsetDateTime now = OffsetDateTime.now();

        when(_jpaTableRepo.findAvailableTablesInTimeframe(now, now.plusHours(1)))
                .thenReturn(List.of(table));

        boolean result = _tableRepo.isTableAvailable("T1", now, now.plusHours(1));

        assertTrue(result);
    }
}