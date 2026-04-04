package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IIngredientsServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientsServices implements IIngredientsServices {
    private final IIngredientsRepository _ingredientsRepo;
    private final IAllergensRepository _allergensRepo;
    private final IDishRepository _dishRepo;

    @Transactional
    @Override
    @Auditable(action = "ADD_INGREDIENTS")
    @CacheEvict(value = "ingredientsDictionary", allEntries = true)
    public void add(AddIngredientRequest request) {
        Ingredients ingredient = DictionaryHelper.createEntity(
                Ingredients::new,
                request.getEntity(),
                _ingredientsRepo::isNameTaken,
                "Ingredient already exists"
        );

        List<String> allergenTokens = request.getAllergenTokens() != null
                ? new ArrayList<>(request.getAllergenTokens())
                : new ArrayList<>();

        var allergens = _allergensRepo.findAllergens(allergenTokens);

        if (allergenTokens.size() != allergens.size()) {
            throw new IllegalStateException("One or more allergens from the provided list do not exist");
        }

        ingredient.setAllergens(new HashSet<>(allergens));

        _ingredientsRepo.save(ingredient);
    }

    @Transactional
    @Override
    @Auditable(action = "REMOVE_INGREDIENTS")
    @Caching(evict = {
            @CacheEvict(value = "ingredientsDictionary", allEntries = true),
            @CacheEvict(value = "dishMenu", allEntries = true)
    })
    public void remove(String token) {
        DictionaryHelper.deleteEntity(
                token,
                _ingredientsRepo::findByToken,
                _ingredientsRepo::save,
                i -> {
                    String orginaNameEn = i.getNameEn();

                    var affectedDishes = _dishRepo.findByIngredientsId(i.getId());

                    for (Dishes dish : affectedDishes) {
                        dish.setAvailable(false);
                        dish.setUnavailableReason(orginaNameEn + " is deleted");
                        _dishRepo.save(dish);
                    }
                }
        );
    }

    @Override
    @Cacheable(
            value = "ingredientsDictionary",
            key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public List<EntityResponse> getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return DictionaryHelper.map(_ingredientsRepo.findAll(), lang);
    }
}
