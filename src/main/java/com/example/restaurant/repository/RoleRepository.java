package com.example.restaurant.repository;

import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoleRepository implements IRoleRepository {
    private final IJpaRoleRepository _jpaRoleRepository;

    public Roles setRole(String role)
    {
        return _jpaRoleRepository.findByName(role)
                .orElseThrow(() -> new RuntimeException("Role not found: " + role));
    }

    public boolean isRoleExists(String role)
    {
        return _jpaRoleRepository.findByName(role).isPresent();
    }

}
