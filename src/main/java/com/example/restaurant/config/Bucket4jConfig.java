package com.example.restaurant.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class Bucket4jConfig {

  @Bean
  public ProxyManager<byte[]> proxyManager(RedisConnectionFactory redisConnectionFactory) {
    RedisClient nativeClient =
        (RedisClient) ((LettuceConnectionFactory) redisConnectionFactory).getNativeClient();

    assert nativeClient != null;
    return LettuceBasedProxyManager.builderFor(nativeClient)
        .withExpirationStrategy(
            io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
                .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1)))
        .build();
  }
}
