package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface ITableRespository {
    boolean isTableExist(String token);

    RestaurantTables findByToken(String token);

    List<RestaurantTables> findAllTables(OffsetDateTime startTime, OffsetDateTime endTime);

    TableStatus findStatusByToken(String token);

    void save(RestaurantTables table);

    boolean isTableAvailable(String tableToken, OffsetDateTime startTime, OffsetDateTime endTime);

    boolean existsByTableNumber(int tableNumber);
    
}
