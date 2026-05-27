package com.example.restaurant.repository;

import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableStatusRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TableRespository implements ITableRespository {
  private final IJpaTableRepository _jpaTableRepo;
  private final IJpaTableStatusRepository _jpaTableStatusRepo;

  @Override
  public boolean isTableExist(String token) {
    return _jpaTableRepo.findByToken(token).isPresent();
  }

  @Override
  public RestaurantTables findByToken(String token) {
    return _jpaTableRepo
        .findByToken(token)
        .orElseThrow(() -> new EntityNotFoundException("Table not found"));
  }

  @Override
  public TableStatus findStatusByToken(String token) {
    return _jpaTableStatusRepo
        .findByToken(token)
        .orElseThrow(() -> new EntityNotFoundException("Table status not found"));
  }

  @Override
  public void save(RestaurantTables table) {
    _jpaTableRepo.save(table);
  }

  @Override
  public boolean isTableAvailable(
      String tableToken, OffsetDateTime startTime, OffsetDateTime endTime) {
    return _jpaTableRepo.findAvailableTablesInTimeframe(startTime, endTime).stream()
        .anyMatch(table -> table.getToken().equals(tableToken));
  }

  @Override
  public List<RestaurantTables> findAll() {
    return _jpaTableRepo.findAll();
  }

  @Override
  public Page<RestaurantTables> findAll(Pageable pageable) {
    return _jpaTableRepo.findAll(pageable);
  }

  @Override
  public List<RestaurantTables> findAvailableTablesInTimeframe(
      OffsetDateTime startTime, OffsetDateTime endTime) {
    return _jpaTableRepo.findAvailableTablesInTimeframe(startTime, endTime);
  }

  @Override
  public boolean existsByTableNumber(int tableNumber) {
    return _jpaTableRepo.existsByTableNumber(tableNumber);
  }

  @Override
  public List<TableStatus> findAllStatuses() {
    return _jpaTableStatusRepo.findAll();
  }

  @Override
  public boolean isStatusNameTaken(String pl, String en) {
    return _jpaTableStatusRepo.findByNamePl(pl).isPresent()
        || _jpaTableStatusRepo.findByNameEn(en).isPresent();
  }

  @Override
  public void saveStatus(TableStatus status) {
    _jpaTableStatusRepo.save(status);
  }

  @Override
  public List<RestaurantTables> findTablesByStatus(TableStatus status) {
    return _jpaTableRepo.findByTableStatusContaining(status);
  }

  @Override
  public long countStatuses() {
    return _jpaTableStatusRepo.count();
  }

  @Override
  public long count() {
    return _jpaTableRepo.count();
  }

  @Override
  public Optional<RestaurantTables> findById(UUID id) {
    return _jpaTableRepo.findById(id);
  }
}
