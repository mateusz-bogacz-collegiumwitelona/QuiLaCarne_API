package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableStatusRepository;
import org.junit.jupiter.api.DisplayName;
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
class TableRepositoryTest {

    @Mock
    private IJpaTableRepository _jpaTableRepo;

    @Mock
    private IJpaTableStatusRepository _jpaTableStatusRepo;

    @InjectMocks
    private TableRespository _tableRepo;

    @Test
    @DisplayName("find table: should return table when found")
    void findByToken_ShouldReturnTable_WhenFound() {
        RestaurantTables mockTable = new RestaurantTables();
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.of(mockTable));

        RestaurantTables result = _tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN);

        assertNotNull(result);
        verify(_jpaTableRepo).findByToken(TestConstants.FAKE_TABLE_TOKEN);
    }

    @Test
    @DisplayName("find table: should throw exception when table not found")
    void findByToken_ShouldThrowTableNotFoundException_WhenNotFound() {
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                _tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)
        );

        assertEquals("Table not found", ex.getMessage());
    }

    @Test
    @DisplayName("is table exist: should return true if exist")
    void isTableExist_ShouldReturnTrue_WhenTableExists() {
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.of(new RestaurantTables()));

        assertTrue(_tableRepo.isTableExist(TestConstants.FAKE_TABLE_TOKEN));
    }

    @Test
    @DisplayName("is table exist: should return false if doesn't exist")
    void isTableExist_ShouldReturnFalse_WhenTableDoesNotExist() {
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN))
                .thenReturn(Optional.empty());

        assertFalse(_tableRepo.isTableExist(TestConstants.FAKE_TABLE_TOKEN));
    }

    @Test
    @DisplayName("is status by token: should return status if found")
    void findStatusByToken_ShouldReturnStatus_WhenFound() {
        TableStatus mockStatus = new TableStatus();
        when(_jpaTableStatusRepo.findByToken("CLEANING"))
                .thenReturn(Optional.of(mockStatus));

        TableStatus result = _tableRepo.findStatusByToken("CLEANING");

        assertNotNull(result);
        verify(_jpaTableStatusRepo).findByToken("CLEANING");
    }

    @Test
    @DisplayName("is status by token: should throw exception if status not found")
    void findStatusByToken_ShouldThrowTableStatusNotFoundException_WhenNotFound() {
        when(_jpaTableStatusRepo.findByToken("CLEANING"))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                _tableRepo.findStatusByToken("CLEANING")
        );

        assertEquals("Table status not found", ex.getMessage());
    }

    @Test
    @DisplayName("save: should save JPA")
    void save_ShouldCallJpaSave() {
        RestaurantTables table = new RestaurantTables();
        _tableRepo.save(table);
        verify(_jpaTableRepo).save(table);
    }

    @Test
    @DisplayName("is tale available: should return true if is available")
    void isTableAvailable_ShouldReturnTrue_WhenTableIsInAvailableList() {
        RestaurantTables table = new RestaurantTables();
        table.setToken(TestConstants.FAKE_TABLE_TOKEN);
        OffsetDateTime now = OffsetDateTime.now();

        when(_jpaTableRepo.findAvailableTablesInTimeframe(now, now.plusHours(1)))
                .thenReturn(List.of(table));

        boolean result = _tableRepo.isTableAvailable(TestConstants.FAKE_TABLE_TOKEN, now, now.plusHours(1));

        assertTrue(result);
    }

    @Test
    @DisplayName("exists By Table Number: should return true when table exists in JPA")
    void existsByTableNumber_ShouldReturnTrue_WhenTableExists() {
        when(_jpaTableRepo.existsByTableNumber(10)).thenReturn(true);

        boolean exists = _tableRepo.existsByTableNumber(10);

        assertTrue(exists);
        verify(_jpaTableRepo, times(1)).existsByTableNumber(10);
    }

    @Test
    @DisplayName("findAllStatuses: Should return list of table statuses from JPA")
    void findAllStatuses_ShouldReturnListOfStatuses() {
        List<TableStatus> expectedStatuses = List.of(new TableStatus(), new TableStatus());
        when(_jpaTableStatusRepo.findAll()).thenReturn(expectedStatuses);

        List<TableStatus> result = _tableRepo.findAllStatuses();

        assertEquals(expectedStatuses, result);
        verify(_jpaTableStatusRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("isStatusNameTaken: Should return true and short-circuit when PL name exists")
    void isStatusNameTaken_ShouldReturnTrue_WhenPlNameExists() {
        when(_jpaTableStatusRepo.findByNamePl(anyString())).thenReturn(Optional.of(new TableStatus()));

        boolean result = _tableRepo.isStatusNameTaken("Status PL", "Status EN");

        assertTrue(result);
        verify(_jpaTableStatusRepo, times(1)).findByNamePl("Status PL");
        verify(_jpaTableStatusRepo, never()).findByNameEn(anyString());
    }

    @Test
    @DisplayName("isStatusNameTaken: Should return true when EN name exists")
    void isStatusNameTaken_ShouldReturnTrue_WhenEnNameExists() {
        when(_jpaTableStatusRepo.findByNamePl(anyString())).thenReturn(Optional.empty());
        when(_jpaTableStatusRepo.findByNameEn(anyString())).thenReturn(Optional.of(new TableStatus()));

        boolean result = _tableRepo.isStatusNameTaken("Status PL", "Status EN");

        assertTrue(result);
        verify(_jpaTableStatusRepo, times(1)).findByNamePl("Status PL");
        verify(_jpaTableStatusRepo, times(1)).findByNameEn("Status EN");
    }

    @Test
    @DisplayName("isStatusNameTaken: Should return false when both names are available")
    void isStatusNameTaken_ShouldReturnFalse_WhenBothAreAvailable() {
        when(_jpaTableStatusRepo.findByNamePl(anyString())).thenReturn(Optional.empty());
        when(_jpaTableStatusRepo.findByNameEn(anyString())).thenReturn(Optional.empty());

        boolean result = _tableRepo.isStatusNameTaken("Status PL", "Status EN");

        assertFalse(result);
    }

    @Test
    @DisplayName("saveStatus: Should call JPA save")
    void saveStatus_ShouldCallJpaSave() {
        TableStatus status = new TableStatus();
        _tableRepo.saveStatus(status);
        verify(_jpaTableStatusRepo, times(1)).save(status);
    }

    @Test
    @DisplayName("findTablesByStatus: Should return list of tables containing given status")
    void findTablesByStatus_ShouldReturnListOfTables() {
        TableStatus status = new TableStatus();
        List<RestaurantTables> expectedTables = List.of(new RestaurantTables(), new RestaurantTables());

        when(_jpaTableRepo.findByTableStatusContaining(status)).thenReturn(expectedTables);

        List<RestaurantTables> result = _tableRepo.findTablesByStatus(status);

        assertEquals(2, result.size());
        verify(_jpaTableRepo, times(1)).findByTableStatusContaining(status);
    }
}