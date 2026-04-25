package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.services.interfaces.ISystemServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemServices implements ISystemServices {
    private final CacheManager _cacheManager;

    @Override
    @Auditable(action = "CLEAR_CACHE")
    public void clearAllCache() {
        _cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = _cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("Cleared cache: {}", cacheName);
            }
        });
    }

    @Override
    @Auditable(action = "CLEAR_SPECIFIC_CACHE")
    public void clearSpecificCache(String cacheName) {
        Cache cache = _cacheManager.getCache(cacheName);

        if (cache == null) throw new EntityNotFoundException("Cache not found");

        cache.clear();
        log.info("Cleared specific cache: {}", cacheName);
    }

    @Override
    public List<String> getCacheList() {
        return _cacheManager.getCacheNames().stream().toList();
    }
}
