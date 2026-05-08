package com.example.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass")
public class QuiLaCarneApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(QuiLaCarneApiApplication.class, args);
  }
}
