package com.example.restaurant.contlollers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/test", produces = "application/json")
public class TestContloller {
    @GetMapping
    public String Test() {
        return "Hello World";
    }
}
