package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.payload.TablePayload;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.AddTableRequest;
import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.SyncDictionaryResponse;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.dto.response.TableListWrapperResponse;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.services.interfaces.ITableServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final NotificationServices _notification;

    private final SyncMapper _syncMapper;

    private static final String TABLE_ENTITY_TYPE = "TABLE";
    private static final String TABLE_STATUS_ENTITY_TYPE = "TABLE_STATUS";

    @Override
    @Cacheable(
            value = "tablesList",
            key = "#request.toString() + '-' + " +
                    "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public TableListWrapperResponse getTables(TableFilterRequest request) {
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                throw new IllegalStateException("Start time cannot be after end time");
            }
        }

        String lang = LocaleContextHolder.getLocale().getLanguage();
        boolean isAvailabilityCheck = (request.getStartTime() != null && request.getEndTime() != null);

        List<RestaurantTables> tables;
        if (isAvailabilityCheck) {
            tables = _tableRepo.findAvailableTablesInTimeframe(request.getStartTime(), request.getEndTime());
        } else {
            tables = _tableRepo.findAll();
        }

        if (tables == null || tables.isEmpty()) {
            return new TableListWrapperResponse(new ArrayList<>());
        }

        List<TableListResponse> response = tables.stream().map(table -> {
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

        return new TableListWrapperResponse(response);
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_TABLE_STATUS_TO_CLEAN")
    @CacheEvict(value = "tablesList", allEntries = true)
    public void changeStatusToClean(String tableToken) {
        String CLEANING_STATUS = "CLEANING";
        changeStatus(tableToken, CLEANING_STATUS);
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_TABLE_STATUS_TO_OUT_OF_SERVICE")
    @CacheEvict(value = "tablesList", allEntries = true)
    public void changeStatusToOutOfService(String tableToken) {
        String OUT_OF_SERVICE = "OUT_OF_SERVICE";
        changeStatus(tableToken, OUT_OF_SERVICE);
    }

    @Override
    @Transactional
    @Auditable(action = "ADD_TABLE")
    @CacheEvict(value = "tablesList", allEntries = true)
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

        WebSocketEvent<TablePayload> event = WebSocketEvent.created(
                TABLE_ENTITY_TYPE, table.getToken(),
                createTablePayload(table)
        );
        _notification.sendEventToTopic("/tables/updates", event);
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_TABLE")
    @CacheEvict(value = "tablesList", allEntries = true)
    public void delete(String token) {
        RestaurantTables table = _tableRepo.findByToken(token);

        table.setDeletedAt(OffsetDateTime.now());

        _tableRepo.save(table);

        WebSocketEvent<Void> event = WebSocketEvent.deleted(TABLE_ENTITY_TYPE, token);
        _notification.sendEventToTopic("/tables/updates", event);
    }

    @Override
    @Cacheable(
            value = "tableStatuses",
            key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public DictionaryResponse getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return new DictionaryResponse(DictionaryHelper.map(_tableRepo.findAllStatuses(), lang));
    }

    @Override
    @Transactional
    @Auditable(action = "ADD_TABLE_STATUS")
    @CacheEvict(value = "tableStatuses", allEntries = true)
    public void addStatus(AddEntityRequest request) {
        TableStatus status = DictionaryHelper.createEntity(
                TableStatus::new,
                request,
                _tableRepo::isStatusNameTaken,
                "Table status already exists"
        );

        _tableRepo.saveStatus(status);

        SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(status);
        WebSocketEvent<SyncDictionaryResponse> event = WebSocketEvent.created(
                TABLE_STATUS_ENTITY_TYPE,
                status.getToken(),
                payload
        );
        _notification.sendEventToTopic("/dictionary/table-statuses", event);
    }

    @Override
    @Transactional
    @Auditable(action = "REMOVE_TABLE_STATUS")
    @CacheEvict(value = "tableStatuses", allEntries = true)
    public void removeStatus(String token) {
        DictionaryHelper.deleteEntity(
                token,
                _tableRepo::findStatusByToken,
                _tableRepo::saveStatus,
                statusToRemove -> {
                    TableStatus fallbackStatus = _tableRepo.findStatusByToken("AVAILABLE");

                    List<RestaurantTables> affectedTables = _tableRepo.findTablesByStatus(statusToRemove);

                    for (RestaurantTables table : affectedTables) {
                        table.getTableStatus().remove(statusToRemove);
                        table.getTableStatus().add(fallbackStatus);
                        _tableRepo.save(table);
                    }
                }
        );

        WebSocketEvent<Void> event = WebSocketEvent.deleted(TABLE_STATUS_ENTITY_TYPE, token);
        _notification.sendEventToTopic("/dictionary/table-statuses", event);
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

        WebSocketEvent<TablePayload> event = WebSocketEvent.updated(
                TABLE_ENTITY_TYPE,
                table.getToken(),
                createTablePayload(table)
        );
        _notification.sendEventToTopic("/tables/updates", event);
    }

    private TablePayload createTablePayload(RestaurantTables table) {
        return TablePayload.builder()
                .token(table.getToken())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .statusTokens(table.getTableStatus() != null
                        ? table.getTableStatus().stream().map(BaseEntity::getToken).toList()
                        : List.of())
                .build();
    }
}
