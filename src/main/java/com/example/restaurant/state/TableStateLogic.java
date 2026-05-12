package com.example.restaurant.state;

import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import java.util.HashSet;
import java.util.Set;

public enum TableStateLogic {
  AVAILABLE {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void release(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Stolik jest już wolny.");
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Wolny stolik nie wymaga sprzątania.");
    }
  },

  RESERVED {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Stolik jest już zarezerwowany.");
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void release(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Najpierw anuluj rezerwację.");
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Zarezerwowany stolik nie powinien być sprzątany.");
    }
  },

  OCCUPIED {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Nie można zarezerwować zajętego stolika.");
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Stolik jest już zajęty.");
    }

    @Override
    public void release(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Zajęty stolik musi najpierw zostać posprzątany.");
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Nie można wyłączyć z użytku zajętego stolika.");
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }
  },

  CLEANING {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Nie można rezerwować stolika w trakcie sprzątania.");
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Najpierw posprzątaj stolik, by go zająć.");
    }

    @Override
    public void release(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Stolik jest już w trakcie sprzątania.");
    }
  },

  OUT_OF_SERVICE {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Stolik wyłączony z użytku.");
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Stolik wyłączony z użytku.");
    }

    @Override
    public void release(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Stolik jest już wyłączony z użytku.");
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Stolik wyłączony z użytku.");
    }
  },

  OTHER {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Niedozwolona operacja.");
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Niedozwolona operacja.");
    }

    @Override
    public void release(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Niedozwolona operacja.");
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Niedozwolona operacja.");
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Niedozwolona operacja.");
    }
  };

  public abstract void reserve(RestaurantTables table, TableStatus newStatus);

  public abstract void occupy(RestaurantTables table, TableStatus newStatus);

  public abstract void release(RestaurantTables table, TableStatus newStatus);

  public abstract void takeOutOfService(RestaurantTables table, TableStatus newStatus);

  public abstract void markAsCleaning(RestaurantTables table, TableStatus newStatus);

  public static TableStateLogic from(RestaurantTables table) {
    if (table.getTableStatus() == null || table.getTableStatus().isEmpty()) {
      return OTHER;
    }
    String token = table.getTableStatus().iterator().next().getToken();
    try {
      return TableStateLogic.valueOf(token);
    } catch (IllegalArgumentException e) {
      return OTHER;
    }
  }
}
