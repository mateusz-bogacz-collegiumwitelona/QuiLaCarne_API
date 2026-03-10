package com.example.restaurant.repository;

import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TableRespository implements ITableRespository {
    private final IJpaTableRepository _jpaTableRepo;

    @Override
    public List<TableListResponse> findAllTables(String lang, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<RestaurantTables> tables;
        boolean isAvailabilityCheck = (startTime != null && endTime != null);


        if (isAvailabilityCheck) {
            tables = _jpaTableRepo.findAvailableTablesInTimeframe(startTime, endTime);
        } else {
            tables = _jpaTableRepo.findAll();
        }

        List<TableListResponse> responses = new ArrayList<>();

        for (RestaurantTables table : tables) {
            String statusName = "UNKNOWN";

            if (table.getTableStatus() != null && !table.getTableStatus().isEmpty()) {
                var status = table.getTableStatus().iterator().next();
                statusName = "pl".equalsIgnoreCase(lang) ? status.getNamePl() : status.getNameEn();
            }

            if (isAvailabilityCheck)
                statusName = "pl".equalsIgnoreCase(lang) ? "Wolny" : "Available";

            TableListResponse response = TableListResponse.builder()
                    .token(table.getToken())
                    .tableNumber(table.getTableNumber())                     .capacity(table.getCapacity())
                    .status(statusName)
                    .updatedAt(table.getUpdatedAt())
                    .build();

            responses.add(response);
        }

        return responses;
    }
}
