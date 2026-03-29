package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IIngredientsServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class IngredientsServices implements IIngredientsServices {
    private final IIngredientsRepository _ingredientsRepo;
    private final IAllergensRepository _allergensRepo;
    private final IDishRepository _dishRepo;

    @Transactional
    @Override
    @Auditable(action = "ADD_INGREDIENTS")
    public ResultHandler<Void> add(AddIngredientRequest request) {
        if (_ingredientsRepo.isNameTaken(request.getEntity().getNamePl(), request.getEntity().getNameEn()))
            throw new EntityAlreadyExistsException("Ingredient already exists");

        var allergenTokens = request.getAllergenTokens() != null ? new ArrayList<>(request.getAllergenTokens()) : new ArrayList<String>();
        var allergens = _allergensRepo.findAllergens(allergenTokens);

        int requestedSize = request.getAllergenTokens() != null ? request.getAllergenTokens().size() : 0;
        if (allergens.size() != requestedSize)
            throw new RuntimeException("One or more allergens not found");

        Ingredients ingredients = new Ingredients();
        ingredients.setNamePl(request.getEntity().getNamePl());
        ingredients.setNameEn(request.getEntity().getNameEn());
        ingredients.setToken(request.getEntity().getNameEn().trim().toUpperCase().replace(" ", "_"));
        ingredients.setAllergens(new HashSet<>(allergens));

        _ingredientsRepo.save(ingredients);

        return ResultHandler.success(
                "Ingredient created successful",
                HttpStatus.CREATED.value()
        );
    }

    @Transactional
    @Override
    @Auditable(action = "REMOVE_INGREDIENTS")
    public ResultHandler<Void> remove(String token) {
        var ingredient = _ingredientsRepo.findByToken(token);

        String timestamp = String.valueOf(System.currentTimeMillis());
        String orginaNameEn = ingredient.getNameEn();

        ingredient.setToken("DELETED_" + timestamp + "_" + ingredient.getToken());
        ingredient.setNameEn("DELETED_" + timestamp + "_" + ingredient.getNameEn());
        ingredient.setNamePl("DELETED_" + timestamp + "_" + ingredient.getNamePl());
        ingredient.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));

        _ingredientsRepo.save(ingredient);

        var affectedDishes = _dishRepo.findByIngredientsId(ingredient.getId());

        for (Dishes dish : affectedDishes) {
            dish.setAvailable(false);
            dish.setUnavailableReason(orginaNameEn + " is deleted");
            _dishRepo.save(dish);
        }

        return ResultHandler.success(
                "Ingredient remove successfuly",
                HttpStatus.OK.value()
        );
    }

}
