package com.example.restaurant.dto.domain;

import java.util.Map;

public record LogDomain(
    String username, String action, String ipAddress, Map<String, Object> details) {}
