package com.example.restaurant.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                // 24h
                .withCacheConfiguration("tableStatuses",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)))

                // 24h
                .withCacheConfiguration("dishCategories",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)))

                // 15s
                .withCacheConfiguration("tablesList",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(15)))

                // 5 min
                .withCacheConfiguration("dishMenu",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))

                // 1h
                .withCacheConfiguration("usersList",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)));
    }
}
