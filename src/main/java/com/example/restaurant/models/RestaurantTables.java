package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.lookup.TableStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "restaurant_tables")
@Getter
@Setter
@SQLRestriction("deleted_at IS NULL")
public class RestaurantTables extends BaseEntity {
  @Column(name = "table_number")
  private int tableNumber;

  @Column(name = "capacity")
  private int capacity;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "x_table_status",
      joinColumns = @JoinColumn(name = "table_id"),
      inverseJoinColumns = @JoinColumn(name = "table_status_id"))
  private Set<TableStatus> tableStatus = new HashSet<>();
}
