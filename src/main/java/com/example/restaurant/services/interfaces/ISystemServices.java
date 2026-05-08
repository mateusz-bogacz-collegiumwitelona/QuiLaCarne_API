package com.example.restaurant.services.interfaces;

import java.util.List;

public interface ISystemServices {
  void clearAllCache();

  void clearSpecificCache(String cacheName);

  List<String> getCacheList();
}
