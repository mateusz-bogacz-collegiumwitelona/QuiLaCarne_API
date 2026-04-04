package com.example.restaurant.enums;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;

import java.time.Duration;

public enum RateLimitEnum {
    GUEST(50), // 50 requests per minute
    CLIENT(100), // 60 requests per minute
    STAFF(200); // 200 requests per minute

    private final int _bucketSize;

    RateLimitEnum(int bucketSize) {
        _bucketSize = bucketSize;
    }

    public BucketConfiguration getConfiguration() {
        Bandwidth limit = Bandwidth.classic(_bucketSize, Refill.intervally(_bucketSize, Duration.ofMinutes(1)));
        return BucketConfiguration.builder().addLimit(limit).build();
    }
}