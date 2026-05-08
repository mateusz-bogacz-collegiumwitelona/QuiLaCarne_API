package com.example.restaurant.helpers;

public final class SoftDeleteHelpers {
  private SoftDeleteHelpers() {
    throw new IllegalStateException("Utility class");
  }

  public static String markAsDelete(String value) {
    if (value == null) return null;
    return "DELETED_" + System.currentTimeMillis() + "_" + value;
  }
}
