package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Dishes;
import com.example.restaurant.repository.interfaces.jpa.base.IJpaNamedEntityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IJpaDishRepository extends IJpaNamedEntityRepository<Dishes> {
    @Query("""
                SELECT d FROM Dishes d
                WHERE NOT EXISTS (
                    SELECT 1 FROM d.ingredients i
                    JOIN i.allergens a
                    WHERE a.token IN :excludedAllergens
                )
            """)
    Page<Dishes> findWithoutAllergens(
            @Param("excludedAllergens") List<String> excludedAllergens,
            Pageable pagable
    );

    List<Dishes> findAllByTokenIn(List<String> tokens);

    List<Dishes> findByIngredientsId(UUID ingredientId);
}
