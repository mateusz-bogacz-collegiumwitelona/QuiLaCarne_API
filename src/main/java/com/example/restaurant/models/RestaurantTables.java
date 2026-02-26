package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.named.TableStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "restaurant_tables")
@Getter
@Setter
public class RestaurantTables extends BaseEntity {
    @Column(name = "table_number")
    private int tableNumber;

    @Column(name = "capacity")
    private int capacity;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "x_table_status",
            joinColumns = @JoinColumn(name = "table_id"),
            inverseJoinColumns = @JoinColumn(name = "table_status_id")
    )
    private Set<TableStatus> tableStatus = new HashSet<>();
}
