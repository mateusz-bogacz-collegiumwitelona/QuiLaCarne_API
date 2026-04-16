package com.example.restaurant.repository;

import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaDishRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaDishesCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class DishRepository implements IDishRepository {
    private final IJpaDishRepository _jpaDishRepo;
    private final IJpaDishesCategoryRepository _jpaDishCategoryRepo;

    @Override
    public Page<Dishes> findAll(Pageable pageable) {
        return _jpaDishRepo.findAll(pageable);
    }

    @Override
    public List<Dishes> findAll() {
        return _jpaDishRepo.findAll();
    }

    @Override
    public Page<Dishes> findWithoutAllergens(List<String> excludedAllergens, Pageable pageable) {
        return _jpaDishRepo.findWithoutAllergens(excludedAllergens, pageable);
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
                .orElseThrow(() -> new EntityNotFoundException("Dish not found"));
    }

    @Override
    public List<Dishes> findByIngredientsId(UUID id) {
        return _jpaDishRepo.findByIngredientsId(id);
    }

    @Override
    public void save(Dishes dish) {
        _jpaDishRepo.save(dish);
    }

    @Override
    public Dishes findByToken(String token) {
        return _jpaDishRepo.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Dish not found"));
    }

    @Override
    public DishesCategories findCategoryByToken(String token) {
        return _jpaDishCategoryRepo.findByToken(token)
                .orElseThrow(
                        () -> new EntityNotFoundException("Dish category not found")
                );
    }

    @Override
    public List<DishesCategories> findAllCategories() {
        return _jpaDishCategoryRepo.findAll();
    }

    @Override
    public boolean isCategoryNameTaken(String pl, String en) {
        return _jpaDishCategoryRepo.findByNamePl(pl).isPresent() ||
                _jpaDishCategoryRepo.findByNameEn(en).isPresent();
    }

    @Override
    public void saveCategory(DishesCategories categorie) {
        _jpaDishCategoryRepo.save(categorie);
    }

    @Override
    public List<Dishes> findByCategoryId(UUID id) {
        return _jpaDishRepo.findByCategoryId(id);
    }

    @Override
    public long countCategories() {
        return _jpaDishCategoryRepo.count();
    }

    @Override
    public long count() {
        return _jpaDishRepo.count();
    }
}
