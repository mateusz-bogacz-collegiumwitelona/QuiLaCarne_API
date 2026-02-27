package com.example.restaurant.repository.interfaces.jpa.base;

import com.example.restaurant.models.base.BaseTranslatedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface IJpaTranslatedRepository<T extends BaseTranslatedEntity> extends JpaRepository<T, UUID> {
    Optional<T> findByToken(String token);
    Optional<T> findByNamePl(String namePl);
    Optional<T> findByNameEn(String nameEn);
}
