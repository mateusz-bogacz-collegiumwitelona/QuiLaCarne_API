package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.AddTableRequest;
import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.dto.response.TableListWrapperResponse;
import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.dto.sync.SyncTableResponse;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.services.interfaces.ITableServices;
import com.example.restaurant.state.TableStateLogic;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

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
      key =
          "#request.toString() + '-' + "
              + "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public TableListWrapperResponse getTables(TableFilterRequest request) {

    validateTimeRange(request);

    String lang = LocaleContextHolder.getLocale().getLanguage();
    List<RestaurantTables> tables = fetchTables(request);

    if (tables.isEmpty()) {
      return new TableListWrapperResponse(List.of());
    }

    List<TableListResponse> response =
        tables.stream()
            .map(table -> mapTable(table, lang, isAvailabilityRequest(request)))
            .toList();

    return new TableListWrapperResponse(response);
  }

  @Override
  @Transactional
  @Auditable(action = "CHANGE_TABLE_STATUS_TO_CLEAN")
  @CacheEvict(value = "tablesList", allEntries = true)
  public void changeStatusToClean(String tableToken) {
    RestaurantTables table = _tableRepo.findByToken(tableToken);
    if (table == null) throw new EntityNotFoundException("Table not found");

    TableStatus cleanStatus = _tableRepo.findStatusByToken("CLEANING");
    if (cleanStatus == null) throw new EntityNotFoundException("Table status 'CLEANING' not found");

    TableStateLogic.from(table).markAsCleaning(table, cleanStatus);

    _tableRepo.save(table);
    publishTableUpdate(table);
  }

  @Override
  @Transactional
  @Auditable(action = "CHANGE_TABLE_STATUS_TO_OUT_OF_SERVICE")
  @CacheEvict(value = "tablesList", allEntries = true)
  public void changeStatusToOutOfService(String tableToken) {
    RestaurantTables table = _tableRepo.findByToken(tableToken);
    if (table == null) throw new EntityNotFoundException("Table not found");

    TableStatus oosStatus = _tableRepo.findStatusByToken("OUT_OF_SERVICE");
    if (oosStatus == null)
      throw new EntityNotFoundException("Table status 'OUT_OF_SERVICE' not found");

    TableStateLogic.from(table).takeOutOfService(table, oosStatus);

    _tableRepo.save(table);
    publishTableUpdate(table);
  }

  @Override
  @Transactional
  @Auditable(action = "ADD_TABLE")
  @CacheEvict(value = "tablesList", allEntries = true)
  public void add(AddTableRequest request) {
    if (_tableRepo.existsByTableNumber(request.getTableNumber()))
      throw new EntityAlreadyExistsException(
          "Table with number " + request.getTableNumber() + " already exists");

    TableStatus status = _tableRepo.findStatusByToken("AVAILABLE");

    if (status == null)
      throw new EntityNotFoundException("Default table status 'AVAILABLE' not found in database");

    RestaurantTables table = new RestaurantTables();
    table.setTableNumber(request.getTableNumber());
    table.setCapacity(request.getCapacity());
    table.setTableStatus(Set.of(status));

    _tableRepo.save(table);

    WebSocketEvent<SyncTableResponse> event =
        WebSocketEvent.created(
            TABLE_ENTITY_TYPE, table.getToken(), _syncMapper.toSyncTableResponse(table));
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
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public DictionaryResponse getDictionary() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    return new DictionaryResponse(DictionaryHelper.map(_tableRepo.findAllStatuses(), lang));
  }

  @Override
  @Transactional
  @Auditable(action = "ADD_TABLE_STATUS")
  @CacheEvict(value = "tableStatuses", allEntries = true)
  public void addStatus(AddEntityRequest request) {
    TableStatus status =
        DictionaryHelper.createEntity(
            TableStatus::new,
            request,
            _tableRepo::isStatusNameTaken,
            "Table status already exists");

    _tableRepo.saveStatus(status);

    SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(status);
    WebSocketEvent<SyncDictionaryResponse> event =
        WebSocketEvent.created(TABLE_STATUS_ENTITY_TYPE, status.getToken(), payload);
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
        });

    WebSocketEvent<Void> event = WebSocketEvent.deleted(TABLE_STATUS_ENTITY_TYPE, token);
    _notification.sendEventToTopic("/dictionary/table-statuses", event);
  }

  private void validateTimeRange(TableFilterRequest request) {
    if (request.getStartTime() != null
        && request.getEndTime() != null
        && request.getStartTime().isAfter(request.getEndTime())) {
      throw new IllegalStateException("Start time cannot be after end time");
    }
  }

  private boolean isAvailabilityRequest(TableFilterRequest request) {
    return request.getStartTime() != null && request.getEndTime() != null;
  }

  private List<RestaurantTables> fetchTables(TableFilterRequest request) {
    if (isAvailabilityRequest(request)) {
      return _tableRepo.findAvailableTablesInTimeframe(
          request.getStartTime(), request.getEndTime());
    }
    return _tableRepo.findAll();
  }

  private String resolveStatusName(RestaurantTables table, String lang) {
    if (table.getTableStatus() == null || table.getTableStatus().isEmpty()) {
      return "UNKNOWN";
    }

    var status = table.getTableStatus().iterator().next();

    return "pl".equalsIgnoreCase(lang) ? status.getNamePl() : status.getNameEn();
  }

  private TableListResponse mapTable(
      RestaurantTables table, String lang, boolean isAvailabilityCheck) {

    String statusName = resolveStatusName(table, lang);

    if (isAvailabilityCheck) {
      statusName = "pl".equalsIgnoreCase(lang) ? "Wolny" : "Available";
    }

    return TableListResponse.builder()
        .token(table.getToken())
        .tableNumber(table.getTableNumber())
        .capacity(table.getCapacity())
        .status(statusName)
        .updatedAt(table.getUpdatedAt())
        .build();
  }

  private void publishTableUpdate(RestaurantTables table) {
    WebSocketEvent<SyncTableResponse> event =
        WebSocketEvent.updated(
            TABLE_ENTITY_TYPE, table.getToken(), _syncMapper.toSyncTableResponse(table));
    _notification.sendEventToTopic("/tables/updates", event);
  }
}
