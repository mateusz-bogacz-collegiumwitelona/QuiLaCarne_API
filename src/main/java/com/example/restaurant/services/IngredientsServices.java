package com.example.restaurant.services;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IIngredientsServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class IngredientsServices implements IIngredientsServices {
    private final IIngredientsRepository _ingredientsRepo;

    @Transactional
    @Override
    public ResultHandler<Void> add(@RequestBody AddEntityRequest request) {
        _ingredientsRepo.add(request);

        return ResultHandler.success(
                "Ingredient created successful",
                HttpStatus.CREATED.value()
        );
    }
}
