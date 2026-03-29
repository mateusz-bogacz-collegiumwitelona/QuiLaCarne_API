package com.example.restaurant.repository;

import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaIngredientsRepository;
import lombok.RequiredArgsConstructor;
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
        return _jpaIngredientsRepo.findByNamePl(pl).isPresent() ||
                _jpaIngredientsRepo.findByNameEn(en).isPresent();
    }

    @Override
    public Ingredients findByToken(String token) {
        return _jpaIngredientsRepo.findByToken(token)
                .orElseThrow(
                        () -> new RuntimeException("Ingredient not found")
                );
    }
}
