package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.helpers.SoftDeleteHelpers;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IIngredientsServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
    public void add(AddIngredientRequest request) {
        String namePl = request.getEntity().getNamePl().trim();
        String nameEn = request.getEntity().getNameEn().trim();

        if (_ingredientsRepo.isNameTaken(namePl, nameEn))
            throw new EntityAlreadyExistsException("Ingredient already exists");

        var allergenTokens = request.getAllergenTokens() != null
                ? new ArrayList<>(request.getAllergenTokens())
                : new ArrayList<String>();

        var allergens = _allergensRepo.findAllergens(allergenTokens);

        if (allergenTokens.size() != allergens.size())
            throw new IllegalStateException("One or more allergens from the provided list do not exist");

        int requestedSize = request.getAllergenTokens() != null ? request.getAllergenTokens().size() : 0;
        if (allergens.size() != requestedSize)
            throw new RuntimeException("One or more allergens not found");

        Ingredients ingredients = new Ingredients();
        ingredients.setNamePl(request.getEntity().getNamePl());
        ingredients.setNameEn(request.getEntity().getNameEn());
        ingredients.setToken(nameEn.toUpperCase().replace(" ", "_"));
        ingredients.setAllergens(new HashSet<>(allergens));

        _ingredientsRepo.save(ingredients);


    }

    @Transactional
    @Override
    @Auditable(action = "REMOVE_INGREDIENTS")
    public void remove(String token) {
        var ingredient = _ingredientsRepo.findByToken(token);

        String orginaNameEn = ingredient.getNameEn();

        ingredient.setToken(SoftDeleteHelpers.markAsDelete(ingredient.getToken()));
        ingredient.setNameEn(SoftDeleteHelpers.markAsDelete(ingredient.getNameEn()));
        ingredient.setNamePl(SoftDeleteHelpers.markAsDelete(ingredient.getNamePl()));
        ingredient.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));

        _ingredientsRepo.save(ingredient);

        var affectedDishes = _dishRepo.findByIngredientsId(ingredient.getId());

        for (Dishes dish : affectedDishes) {
            dish.setAvailable(false);
            dish.setUnavailableReason(orginaNameEn + " is deleted");
            _dishRepo.save(dish);
        }
    }


}
