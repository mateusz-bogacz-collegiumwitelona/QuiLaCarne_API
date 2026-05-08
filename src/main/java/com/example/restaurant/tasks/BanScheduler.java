package com.example.restaurant.tasks;

import com.example.restaurant.services.interfaces.IBanServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BanScheduler {
  private final IBanServices _banServices;

  @Scheduled(cron = "0 * * * * *")
  public void unban() {
    log.debug("Checking for expired bans...");
    _banServices.processExpiredBans();
  }
}
