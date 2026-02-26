package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItems extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Dishes product;

    private int quantity;
    private int priceAtTimeOfOrder;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "x_order_items_status",
            joinColumns = @JoinColumn(name = "order_items_id"),
            inverseJoinColumns = @JoinColumn(name = "order_items_status_id")
    )
    private Set<OrderItemsStatus> statuses = new HashSet<>();
}
