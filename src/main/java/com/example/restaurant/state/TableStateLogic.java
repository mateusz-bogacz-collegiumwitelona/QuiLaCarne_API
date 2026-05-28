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
      throw new IllegalStateException("The table is now free.");
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("A free table does not require cleaning.");
    }
  },

  RESERVED {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("The table is already booked.");
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
      throw new IllegalStateException("Please cancel your reservation first.");
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("A reserved table should not be cleaned.");
    }
  },

  OCCUPIED {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("It is not possible to reserve a table that is already occupied.");
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("The table is already occupied.");
    }

    @Override
    public void release(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("An occupied table must first be cleaned up..");
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("You cannot put an occupied table out of service.");
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }
  },

  CLEANING {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("You cannot reserve a table during cleaning.");
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("First, clear the table to occupy it.");
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
      throw new IllegalStateException("The table is already being cleaned.");
    }
  },

  OUT_OF_SERVICE {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("The table is out of use.");
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("The table is out of use.");
    }

    @Override
    public void release(RestaurantTables table, TableStatus newStatus) {
      table.setTableStatus(new HashSet<>(Set.of(newStatus)));
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("The table is no longer in use.");
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("The table is out of use.");
    }
  },

  OTHER {
    @Override
    public void reserve(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }

    @Override
    public void occupy(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }

    @Override
    public void release(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }

    @Override
    public void takeOutOfService(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
    }

    @Override
    public void markAsCleaning(RestaurantTables table, TableStatus newStatus) {
      throw new IllegalStateException("Illegal operation.");
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
