package com.example.restaurant.config;

import com.example.restaurant.interceptors.IdempotencyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
  private final IdempotencyInterceptor _idempotencyInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(_idempotencyInterceptor)
        .addPathPatterns("/api/reservations/**")
        .addPathPatterns("/api/dishes/**")
        .addPathPatterns("/api/auth/**")
        .addPathPatterns("/api/ban/**")
        .addPathPatterns("/api/dishes/**")
        .addPathPatterns("/api/ingredients/**")
        .addPathPatterns("/api/order/**")
        .addPathPatterns("/api/report/**")
        .addPathPatterns("/api/reservations/**")
        .addPathPatterns("/api/sync/**")
        .addPathPatterns("/api/system/**")
        .addPathPatterns("/api/tables/**")
        .addPathPatterns("/api/user/**");
  }
}
