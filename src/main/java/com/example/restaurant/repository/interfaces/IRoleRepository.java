package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.lookup.Roles;

public interface IRoleRepository {
    public Roles setRole(String role);
    public boolean isRoleExists(String role);
}
