package com.example.restaurant.repository;

import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.exceptions.TableNotFoundException;
import com.example.restaurant.exceptions.TableStatusNotFoundException;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class TableRespository implements ITableRespository {
    private final IJpaTableRepository _jpaTableRepo;
    private final IJpaTableStatusRepository _jpaTableStatusRepo;

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
                    .tableNumber(table.getTableNumber()).capacity(table.getCapacity())
                    .status(statusName)
                    .updatedAt(table.getUpdatedAt())
                    .build();

            responses.add(response);
        }

        return responses;
    }

    @Override
    public boolean isTableExist(String token) {
        RestaurantTables table = _jpaTableRepo.findByToken(token);

        if (table == null)
            return false;

        return true;
    }

    @Override
    public void changeStatus(String token, String statusToken) {
        RestaurantTables table = _jpaTableRepo.findByToken(token);

        if (table == null)
            throw new TableNotFoundException("Table not found");

        TableStatus cleanStatus = _jpaTableStatusRepo.findByToken(statusToken)
                .orElseThrow(() -> new TableStatusNotFoundException("Table status not found"));

        table.setTableStatus(new HashSet<>(Set.of(cleanStatus)));
        
        _jpaTableRepo.save(table);
    }
}