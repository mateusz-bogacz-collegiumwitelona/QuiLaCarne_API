package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

public interface ITableRespository {
    boolean isTableExist(String token);

    RestaurantTables findByToken(String token);

    TableStatus findStatusByToken(String token);

    void save(RestaurantTables table);

    boolean isTableAvailable(String tableToken, OffsetDateTime startTime, OffsetDateTime endTime);

    List<RestaurantTables> findAll();

    Page<RestaurantTables> findAll(Pageable pageable);

    List<RestaurantTables> findAvailableTablesInTimeframe(OffsetDateTime startTime, OffsetDateTime endTime);

    boolean existsByTableNumber(int tableNumber);

    List<TableStatus> findAllStatuses();

    boolean isStatusNameTaken(String pl, String en);

    void saveStatus(TableStatus status);

    List<RestaurantTables> findTablesByStatus(TableStatus status);

    long countStatuses();

    long count();


}
