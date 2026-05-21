package com.example.restaurant.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

  @Mock private CacheManager cacheManager;

  @Mock private Cache cache;

  @InjectMocks private IdempotencyService idempotencyService;

  private static final String CACHE_NAME = "idempotencyKeys";
  private static final String REQUEST_ID = "123e4567-e89b-12d3-a456-426614174000";

  @BeforeEach
  void setUp() {
    when(cacheManager.getCache(CACHE_NAME)).thenReturn(cache);
  }

  @Test
  @DisplayName("Is processed: should return true when key exist in cache")
  void isProcessed_shouldReturnTrue_whenKeyExistsInCache() {
    Cache.ValueWrapper valueWrapper = () -> true;
    when(cache.get(REQUEST_ID)).thenReturn(valueWrapper);

    boolean result = idempotencyService.isProcessed(REQUEST_ID);

    assertTrue(result);
    verify(cache).get(REQUEST_ID);
  }

  @Test
  @DisplayName("Is processed: should return false when key does not exist")
  void isProcessed_shouldReturnFalse_whenKeyDoesNotExist() {
    when(cache.get(REQUEST_ID)).thenReturn(null);

    boolean result = idempotencyService.isProcessed(REQUEST_ID);

    assertFalse(result);
    verify(cache).get(REQUEST_ID);
  }

  @Test
  @DisplayName("Mark as processed: should put key in cache")
  void markAsProcessed_shouldPutKeyInCache() {
    idempotencyService.markAsProcessed(REQUEST_ID);
    verify(cache).put(REQUEST_ID, true);
  }

  @Test
  @DisplayName("Mark as processed: should do nothing when cache is null")
  void markAsProcessed_shouldDoNothing_whenCacheIsNull() {
    when(cacheManager.getCache(CACHE_NAME)).thenReturn(null);

    idempotencyService.markAsProcessed(REQUEST_ID);

    verify(cache, never()).put(anyString(), any());
  }
}
