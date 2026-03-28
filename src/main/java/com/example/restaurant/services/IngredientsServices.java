package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IIngredientsServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class IngredientsServices implements IIngredientsServices {
    private final IIngredientsRepository _ingredientsRepo;
    private final IAllergensRepository _allergensRepo;

    @Transactional
    @Override
    @Auditable(action = "ADD_INGREDIENTS")
    public ResultHandler<Void> add(AddIngredientRequest request) {
        if (_ingredientsRepo.isNameTaken(request.getEntity().getNamePl(), request.getEntity().getNameEn()))
            throw new EntityAlreadyExistsException("Ingredient already exists");

        var allergens = _allergensRepo.findAllergens(new ArrayList<>(request.getAllergenTokens()));

        if (allergens.size() != request.getAllergenTokens().size())
            throw new RuntimeException("One or more allergens not found");

        Ingredients ingredients = new Ingredients();
        ingredients.setNamePl(request.getEntity().getNamePl());
        ingredients.setNameEn(request.getEntity().getNameEn());
        ingredients.setToken(request.getEntity().getNameEn().trim().toUpperCase().replaceAll(" ", "_"));
        ingredients.setAllergens(new HashSet<>(allergens));

        _ingredientsRepo.save(ingredients);

        return ResultHandler.success(
                "Ingredient created successful",
                HttpStatus.CREATED.value()
        );
    }
}
