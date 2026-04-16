package com.example.restaurant.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisCacheConfiguration baseConfig) {
        return (builder) -> builder
                .withCacheConfiguration("tableStatuses", baseConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("dishCategories", baseConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("tablesList", baseConfig.entryTtl(Duration.ofSeconds(15)))
                .withCacheConfiguration("dishMenu", baseConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("publicDishMenu", baseConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("usersList", baseConfig.entryTtl(Duration.ofHours(1)));
    }
}
