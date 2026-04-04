package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddTableRequest;
import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.services.interfaces.ITableServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TableServices implements ITableServices {
    private final ITableRespository _tableRepo;

    @Override
    public List<TableListResponse> getTables(TableFilterRequest request) {
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                throw new IllegalStateException("Start time cannot be after end time");
            }
        }

        String lang = LocaleContextHolder.getLocale().getLanguage();
        boolean isAvailabilityCheck = (request.getStartTime() != null && request.getEndTime() != null);

        List<RestaurantTables> tables = _tableRepo.findAllTables(request.getStartTime(), request.getEndTime());

        if (tables == null || tables.isEmpty()) {
            return new ArrayList<>();
        }

        return tables.stream().map(table -> {
            String statusName = "UNKNOWN";
            if (table.getTableStatus() != null && !table.getTableStatus().isEmpty()) {
                var status = table.getTableStatus().iterator().next();
                statusName = "pl".equalsIgnoreCase(lang) ? status.getNamePl() : status.getNameEn();
            }

            if (isAvailabilityCheck)
                statusName = "pl".equalsIgnoreCase(lang) ? "Wolny" : "Available";

            return TableListResponse.builder()
                    .token(table.getToken())
                    .tableNumber(table.getTableNumber())
                    .capacity(table.getCapacity())
                    .status(statusName)
                    .updatedAt(table.getUpdatedAt())
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_TABLE_STATUS_TO_CLEAN")
    public void changeStatusToClean(String tableToken) {
        String CLEANING_STATUS = "CLEANING";
        changeStatus(tableToken, CLEANING_STATUS);
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_TABLE_STATUS_TO_OUT_OF_SERVICE")
    public void changeStatusToOutOfService(String tableToken) {
        String OUT_OF_SERVICE = "OUT_OF_SERVICE";
        changeStatus(tableToken, OUT_OF_SERVICE);
    }

    @Override
    @Transactional
    @Auditable(action = "ADD_TABLE")
    public void add(AddTableRequest request) {
        if (_tableRepo.existsByTableNumber(request.getTableNumber()))
            throw new EntityAlreadyExistsException("Table with number " + request.getTableNumber() + " already exists");

        TableStatus status = _tableRepo.findStatusByToken("AVAILABLE");

        if (status == null)
            throw new EntityNotFoundException("Default table status 'AVAILABLE' not found in database");

        RestaurantTables table = new RestaurantTables();
        table.setTableNumber(request.getTableNumber());
        table.setCapacity(request.getCapacity());
        table.setTableStatus(Set.of(status));

        _tableRepo.save(table);
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_TABLE")
    public void delete(String token) {
        RestaurantTables table = _tableRepo.findByToken(token);
        table.setDeletedAt(OffsetDateTime.now());
        _tableRepo.save(table);
    }

    private void changeStatus(String token, String statusToken) {
        RestaurantTables table = _tableRepo.findByToken(token);
        if (table == null) {
            throw new EntityNotFoundException("Table with token " + token + " not found");
        }

        TableStatus cleanStatus = _tableRepo.findStatusByToken(statusToken);
        if (cleanStatus == null) {
            throw new EntityNotFoundException("Table status '" + statusToken + "' not found");
        }

        table.setTableStatus(new HashSet<>(Set.of(cleanStatus)));
        _tableRepo.save(table);
    }

    @Override
    public List<EntityResponse> getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return DictionaryHelper.map(_tableRepo.findAllStatuses(), lang);
    }
}
