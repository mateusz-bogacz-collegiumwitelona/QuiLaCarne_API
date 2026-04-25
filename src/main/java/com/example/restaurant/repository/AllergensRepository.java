package com.example.restaurant.repository;

import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaAllergensRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AllergensRepository implements IAllergensRepository {
    private final IJpaAllergensRepository _jpaAllergensRepo;

    @Override
    public List<Allergens> findAllergens(List<String> allergenTokens) {
        if (allergenTokens == null || allergenTokens.isEmpty()) return new ArrayList<>();

        return _jpaAllergensRepo.findByTokenIn(allergenTokens);
    }

    @Override
    public List<Allergens> findAll() {
        return _jpaAllergensRepo.findAll();
    }

    @Override
    public boolean isNameTaken(String pl, String en) {
        return _jpaAllergensRepo.findByNamePl(pl).isPresent() ||
                _jpaAllergensRepo.findByNameEn(en).isPresent();
    }

    @Override
    public void save(Allergens allergen) {
        _jpaAllergensRepo.save(allergen);
    }

    @Override
    public Allergens findByToken(String token) {
        return _jpaAllergensRepo.findByToken(token)
                .orElseThrow(
                        () -> new EntityNotFoundException("Allergen not found")
                );
    }

    @Override
    public long count() {
        return _jpaAllergensRepo.count();
    }
}
