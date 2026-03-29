package com.example.restaurant.repository;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaDishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DishRepository implements IDishRepository {
    private final IJpaDishRepository _jpaDishRepo;

    @Override
    public Page<Dishes> findAllDishes(DishFilterRequest request, PaggedRequest pagged) {
        Pageable pageable = PageRequest.of(pagged.getPage() - 1, pagged.getSize());

        var excludedAllergens = request.getExcludedAllergens();

        if (excludedAllergens != null && !excludedAllergens.isEmpty()) {
            return _jpaDishRepo.findWithoutAllergens(excludedAllergens, pageable);
        } else {
            return _jpaDishRepo.findAll(pageable);
        }
    }

    @Override
    public List<Dishes> listForOrder(List<String> tokens) {
        return _jpaDishRepo.findAllByTokenIn(tokens);
    }

    @Override
    public Dishes get(List<Dishes> dishes, String token) {
        return dishes.stream()
                .filter(d -> d.getToken().equals(token))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Dish not found"));
    }

    @Override
    public List<Dishes> findByIngredientsId(UUID id) {
        return _jpaDishRepo.findByIngredientsId(id);
    }

    @Override
    public void save(Dishes dish) {
        _jpaDishRepo.save(dish);
    }
}
