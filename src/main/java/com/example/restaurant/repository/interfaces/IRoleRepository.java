package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.lookup.Roles;

import java.util.List;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public interface IRoleRepository {
    Roles setRole(String role);

    boolean isRoleExists(String role);

    long count();

    List<Roles> findAll();
}
