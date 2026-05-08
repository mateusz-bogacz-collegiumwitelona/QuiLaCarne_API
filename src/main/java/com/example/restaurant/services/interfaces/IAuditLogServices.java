package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.LogDomain;

public interface IAuditLogServices {
  void log(LogDomain logDomain);
}
