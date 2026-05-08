package com.example.restaurant.tasks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheSchedulerTest {

  @Test
  @DisplayName("clearAllCache: Should execute method without throwing exceptions")
  void clearAllCache_ShouldExecuteSuccessfully() {
    CacheScheduler scheduler = new CacheScheduler();
    assertDoesNotThrow(scheduler::clearAllCache);
  }
}
