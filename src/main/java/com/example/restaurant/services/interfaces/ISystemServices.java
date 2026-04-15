package com.example.restaurant.services.interfaces;

import java.util.List;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public interface ISystemServices {
    void clearAllCache();

    void clearSpecificCache(String cacheName);

    List<String> getCacheList();
}
