package com.example.restaurant.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
  private final CacheManager _cacheManager;

  private static final String CACHE_NAME = "idempotencyKeys";

  public boolean isProcessed(String requestId) {
    Cache cache = _cacheManager.getCache(CACHE_NAME);
    if (cache != null && cache.get(requestId) != null) {
      log.warn("Idempotency hit! Blocked duplicate request from UUID: {}", requestId);
      return true;
    }
    return false;
  }

  public void markAsProcessed(String requestId) {
    Cache cache = _cacheManager.getCache(CACHE_NAME);
    if (cache != null) {
      cache.put(requestId, true);
      log.debug("Idempotency key saved in Redis: {}", requestId);
    }
  }
}
