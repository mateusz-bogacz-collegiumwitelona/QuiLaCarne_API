package com.example.restaurant.validators.dish;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.repository.interfaces.IDishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExcludeAllergensDishSearchStrategy implements DishSearchStrategy {
  private final IDishRepository _dishRepo;

  @Override
  public boolean supports(DishFilterRequest request) {
    return request.getExcludedAllergens() != null && !request.getExcludedAllergens().isEmpty();
  }

  @Override
  public Page<Dishes> find(DishFilterRequest request, Pageable pageable) {
    return _dishRepo.findWithoutAllergens(request.getExcludedAllergens(), pageable);
  }
}
