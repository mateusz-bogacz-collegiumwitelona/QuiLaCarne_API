package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.lookup.Roles;

public interface IRoleRepository {
    Roles setRole(String role);
    boolean isRoleExists(String role);
}
