package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IIngredientsServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class IngredientsServices implements IIngredientsServices {
    private final IIngredientsRepository _ingredientsRepo;
    private final IAllergensRepository _allergensRepo;

    @Transactional
    @Override
    public ResultHandler<Void> add(AddIngredientRequest request) {
        var allergens = _allergensRepo.findAllergens(new ArrayList<>(request.getAllergenTokens()));

        _ingredientsRepo.add(request.getEntity(), allergens);

        return ResultHandler.success(
                "Ingredient created successful",
                HttpStatus.CREATED.value()
        );
    }
}
