package com.example.restaurant.state;

import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import java.util.HashSet;
import java.util.Set;

public enum OrderItemStateLogic {
  PENDING {
    @Override
    public void startPreparation(OrderItems item, OrderItemsStatus newStatus) {
      item.setStatuses(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void cancel(OrderItems item, OrderItemsStatus newStatus) {
      item.setStatuses(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void markAsReady(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException(
          "You cannot release a dish from the kitchen that has not been started.");
    }

    @Override
    public void serve(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("You cannot serve a guest a dish that is not ready.");
    }
  },

  IN_PROGRESS {
    @Override
    public void startPreparation(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("The dish is already being prepared.");
    }

    @Override
    public void cancel(OrderItems item, OrderItemsStatus newStatus) {
      item.setStatuses(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void markAsReady(OrderItems item, OrderItemsStatus newStatus) {
      item.setStatuses(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void serve(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException(
          "KThe waiter cannot serve a dish that the kitchen is still preparing.");
    }
  },

  READY {
    @Override
    public void startPreparation(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("The dish is now ready to be served.");
    }

    @Override
    public void cancel(OrderItems item, OrderItemsStatus newStatus) {
      item.setStatuses(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void markAsReady(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("The dish is already waiting at the publisher.");
    }

    @Override
    public void serve(OrderItems item, OrderItemsStatus newStatus) {
      item.setStatuses(new HashSet<>(Set.of(newStatus)));
    }
  },

  SERVED {
    @Override
    public void startPreparation(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("The dish has already been served to the guest.");
    }

    @Override
    public void cancel(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException(
          "Once a dish has been served, it cannot be canceled. Use the RETURNED option.");
    }

    @Override
    public void markAsReady(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("The dish has already been served.");
    }

    @Override
    public void serve(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("The dish is already on the table.");
    }
  },

  CANCELLED {
    @Override
    public void startPreparation(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("The dish was canceled.");
    }

    @Override
    public void cancel(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("Already canceled.");
    }

    @Override
    public void markAsReady(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("The dish was canceled.");
    }

    @Override
    public void serve(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("The dish was canceled.");
    }
  },

  OTHER {
    @Override
    public void startPreparation(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }

    @Override
    public void cancel(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }

    @Override
    public void markAsReady(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }

    @Override
    public void serve(OrderItems item, OrderItemsStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }
  };

  public abstract void startPreparation(OrderItems item, OrderItemsStatus newStatus);

  public abstract void cancel(OrderItems item, OrderItemsStatus newStatus);

  public abstract void markAsReady(OrderItems item, OrderItemsStatus newStatus);

  public abstract void serve(OrderItems item, OrderItemsStatus newStatus);

  public static OrderItemStateLogic from(OrderItems item) {
    if (item.getStatuses() == null || item.getStatuses().isEmpty()) {
      return PENDING;
    }
    String token = item.getStatuses().iterator().next().getToken();
    try {
      return OrderItemStateLogic.valueOf(token);
    } catch (IllegalArgumentException e) {
      return OTHER;
    }
  }
}
