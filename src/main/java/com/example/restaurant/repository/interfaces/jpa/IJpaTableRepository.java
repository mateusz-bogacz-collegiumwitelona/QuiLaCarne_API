package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.RestaurantTables;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IJpaTableRepository extends JpaRepository<RestaurantTables, UUID> {
    @Query("""
        SELECT t FROM RestaurantTables t 
        LEFT JOIN t.tableStatus ts 
        WHERE (ts.token IS NULL OR ts.token != 'OUT_OF_SERVICE') 
        AND t.id NOT IN (
            SELECT r.tableId.id FROM Reservations r 
            JOIN r.reservationStatus rs 
            WHERE rs.token = 'ACTIVE' 
            AND r.startTime < :endTime 
            AND r.endTime > :startTime
        )
    """)
    List<RestaurantTables> findAvailableTablesInTimeframe (
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
            );
}
