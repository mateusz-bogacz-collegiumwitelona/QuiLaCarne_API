package com.example.restaurant.repository;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaIngredientsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IngredientsRepository implements IIngredientsRepository {
    private IJpaIngredientsRepository _jpaIngredientsRepo;

    public IngredientsRepository(IJpaIngredientsRepository jpaIngredientsRepo) {
        _jpaIngredientsRepo = jpaIngredientsRepo;
    }

    @Override
    public void add(AddEntityRequest request) {
        if (_jpaIngredientsRepo.findByNamePl(request.getNamePl()).isPresent() ||
                _jpaIngredientsRepo.findByNameEn(request.getNameEn()).isPresent())
            throw new EntityAlreadyExistsException("Name already exists");

        Ingredients ingredients = new Ingredients();
        ingredients.setNamePl(request.getNamePl());
        ingredients.setNameEn(request.getNameEn());
        ingredients.setToken(request.getNameEn().trim().toUpperCase().replaceAll(" ", "_"));

        _jpaIngredientsRepo.saveAndFlush(ingredients);
    }
}
