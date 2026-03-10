package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TableRepositoryTest {
    @Mock
    private IJpaTableRepository _jpaTableRepo;

    @InjectMocks
    private TableRespository _tableRepo;

    @Test
    void findAllTables_ShouldMapEntityToDto_WithTranslation() {
        TableStatus mockStatus = mock(TableStatus.class);
        when(mockStatus.getNamePl()).thenReturn("Wolny");

        RestaurantTables mockTable = new RestaurantTables();
        mockTable.setTableNumber(100);
        mockTable.setCapacity(4);
        mockTable.setToken(TestConstants.FAKE_TOKEN);
        mockTable.setTableStatus(Set.of(mockStatus));

        when(_jpaTableRepo.findAll()).thenReturn(List.of(mockTable));

        var result = _tableRepo.findAllTables("pl", null, null);

        assertEquals(1, result.size());
        var dto = result.get(0);
        assertEquals(TestConstants.FAKE_TOKEN, dto.getToken());
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
        mockTable.setToken(TestConstants.FAKE_TOKEN);
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
}
