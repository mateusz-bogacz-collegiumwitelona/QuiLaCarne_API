package com.example.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class QuiLaCarneApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuiLaCarneApiApplication.class, args);
    }
}
