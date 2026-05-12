package com.example.restaurant.dto.domain;

import java.util.Map;

public record EmailDomain(
    String to, String subject, String template, Map<String, Object> variables) {}
