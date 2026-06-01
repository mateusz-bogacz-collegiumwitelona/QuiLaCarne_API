package com.example.restaurant.repository;

import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaIngredientsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IngredientsRepository implements IIngredientsRepository {
  private final IJpaIngredientsRepository _jpaIngredientsRepo;

  @Override
  public void save(Ingredients ingredients) {
    _jpaIngredientsRepo.save(ingredients);
  }

  @Override
  public boolean isNameTaken(String pl, String en) {
    return _jpaIngredientsRepo.findByNamePl(pl).isPresent()
        || _jpaIngredientsRepo.findByNameEn(en).isPresent();
  }

  @Override
  public Ingredients findByToken(String token) {
    return _jpaIngredientsRepo
        .findByToken(token)
        .orElseThrow(() -> new EntityNotFoundException("Ingredient not found"));
  }

  @Override
  public List<Ingredients> findAll() {
    return _jpaIngredientsRepo.findAll();
  }

  @Override
  public Page<Ingredients> findAll(Pageable pageable) {
    return _jpaIngredientsRepo.findAll(pageable);
  }

  @Override
  public long count() {
    return _jpaIngredientsRepo.count();
  }

  @Override
  public Page<Ingredients> findByDeletedAtIsNull(Pageable pageable) {
    return _jpaIngredientsRepo.findByDeletedAtIsNull(pageable);
  }
}
