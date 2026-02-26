package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.named.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Orders extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private RestaurantTables table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiter_id")
    private Users waiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservations reservation;

    private int totalPrice;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "x_order_status",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "order_status_id")
    )
    private Set<OrderStatus> statuses = new HashSet<>();
}
