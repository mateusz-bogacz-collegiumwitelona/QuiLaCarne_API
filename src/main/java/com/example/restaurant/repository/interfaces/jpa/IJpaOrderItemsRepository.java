package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IJpaOrderItemsRepository extends JpaRepository<OrderItems, UUID> {
  List<OrderItems> findAllByOrder_Token(String orderToken);

  List<OrderItems> findByStatusesContaining(OrderItemsStatus status);
}
