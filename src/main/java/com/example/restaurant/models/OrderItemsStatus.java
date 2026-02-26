package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseNamedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_items_status")
@Getter
@Setter
public class OrderItemsStatus extends BaseNamedEntity {}
