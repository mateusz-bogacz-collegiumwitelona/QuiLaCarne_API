package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.base.BaseNamedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface IJpaLookupRepository<T extends BaseNamedEntity> extends JpaRepository<T, UUID> {
    Optional<T> findByName(String name);
    Optional<T> findByToken(String token);
}
