package com.example.restaurant.validators.dish;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.models.Dishes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DishSearchStrategy {
  boolean supports(DishFilterRequest request);

  Page<Dishes> find(DishFilterRequest request, Pageable pageable);
}
