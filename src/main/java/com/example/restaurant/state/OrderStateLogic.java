package com.example.restaurant.state;

import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderStatus;
import java.util.HashSet;
import java.util.Set;

public enum OrderStateLogic {
  PENDING {
    @Override
    public void assignWaiter(Orders order, OrderStatus newStatus) {
      order.setStatuses(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void cancel(Orders order, OrderStatus newStatus) {
      order.setStatuses(new HashSet<>(Set.of(newStatus)));
    }
  },

  IN_PROGRESS {
    @Override
    public void assignWaiter(Orders order, OrderStatus newStatus) {
      throw new IllegalStateException("This order is already being served by the waiter.");
    }

    @Override
    public void cancel(Orders order, OrderStatus newStatus) {
      order.setStatuses(new HashSet<>(Set.of(newStatus)));
    }
  },

  COMPLETED {
    @Override
    public void assignWaiter(Orders order, OrderStatus newStatus) {
      throw new IllegalStateException("Cannot assign a waiter to a completed order.");
    }

    @Override
    public void cancel(Orders order, OrderStatus newStatus) {
      throw new IllegalStateException("You cannot cancel an order that has been paid/completed.");
    }
  },

  CANCELLED {
    @Override
    public void assignWaiter(Orders order, OrderStatus newStatus) {
      throw new IllegalStateException("The order is canceled.");
    }

    @Override
    public void cancel(Orders order, OrderStatus newStatus) {
      throw new IllegalStateException("The order is already canceled.");
    }
  },

  OTHER {
    @Override
    public void assignWaiter(Orders order, OrderStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }

    @Override
    public void cancel(Orders order, OrderStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }
  };

  public abstract void assignWaiter(Orders order, OrderStatus newStatus);

  public abstract void cancel(Orders order, OrderStatus newStatus);

  public static OrderStateLogic from(Orders order) {
    if (order.getStatuses() == null || order.getStatuses().isEmpty()) {
      return OTHER;
    }
    String token = order.getStatuses().iterator().next().getToken();
    try {
      return OrderStateLogic.valueOf(token);
    } catch (IllegalArgumentException e) {
      return OTHER;
    }
  }
}
