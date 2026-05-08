package com.example.restaurant.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheScheduler {
  @Scheduled(cron = "0 0 3 * * *")
  @CacheEvict(
      value = {
        "allergensDictionary",
        "ingredientsDictionary",
        "dishCategories",
        "dishMenu",
        "orderStatuses",
        "orderItemStatuses",
        "tableStatuses",
        "banStatuses",
        "reportStatuses",
        "reservationStatuses"
      },
      allEntries = true)
  public void clearAllCache() {
    log.info("Cache eviction completed");
  }
}
