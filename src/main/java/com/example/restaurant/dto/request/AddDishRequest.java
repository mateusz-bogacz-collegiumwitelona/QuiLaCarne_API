package com.example.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class AddDishRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Positive(message = "Price must be positive")
    @NotNull(message = "Price cannot be null")
    private int price;

    @NotBlank(message = "Category token cannot be blank")
    private String categoryToken;

    private List<String> ingredientTokens = new ArrayList<>();


    private MultipartFile photo;
}
