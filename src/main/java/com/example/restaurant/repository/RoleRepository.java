package com.example.restaurant.repository;

import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoleRepository implements IRoleRepository {
  private final IJpaRoleRepository _jpaRoleRepository;

  @Override
  public Roles setRole(String role) {
    return _jpaRoleRepository
        .findByName(role)
        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + role));
  }

  @Override
  public boolean isRoleExists(String role) {
    return _jpaRoleRepository.findByName(role).isPresent();
  }

  @Override
  public long count() {
    return _jpaRoleRepository.count();
  }

  @Override
  public List<Roles> findAll() {
    return _jpaRoleRepository.findAll();
  }
}
