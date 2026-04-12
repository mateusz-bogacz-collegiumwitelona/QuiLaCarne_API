package com.example.restaurant.services;

import com.example.restaurant.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemServicesTest {

    @Mock
    private CacheManager _cacheManager;

    @InjectMocks
    private SystemServices _systemServices;

    @Test
    @DisplayName("clearAllCache: Should iterate and clear all existing caches")
    void clearAllCache_ShouldClearAll() {
        when(_cacheManager.getCacheNames()).thenReturn(List.of("cache1", "cache2"));
        Cache mockCache = mock(Cache.class);
        when(_cacheManager.getCache(anyString())).thenReturn(mockCache);

        _systemServices.clearAllCache();

        verify(mockCache, times(2)).clear();
    }

    @Test
    @DisplayName("clearSpecificCache: Should clear named cache if it exists")
    void clearSpecificCache_ShouldClear_WhenFound() {
        Cache mockCache = mock(Cache.class);
        when(_cacheManager.getCache("myCache")).thenReturn(mockCache);

        _systemServices.clearSpecificCache("myCache");

        verify(mockCache, times(1)).clear();
    }

    @Test
    @DisplayName("clearSpecificCache: Should throw exception when cache name is invalid")
    void clearSpecificCache_ShouldThrow_WhenNotFound() {
        when(_cacheManager.getCache("unknown")).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () ->
                _systemServices.clearSpecificCache("unknown")
        );
    }
}