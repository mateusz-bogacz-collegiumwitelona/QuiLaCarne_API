package com.example.restaurant.repository.interfaces.jpa.base;

import com.example.restaurant.models.base.BaseNamedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface IJpaNamedEntityRepository<T extends BaseNamedEntity> extends JpaRepository<T, UUID> {
    Optional<T> findByName(String name);
    Optional<T> findByToken(String token);
}
