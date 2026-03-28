package com.example.restaurant.repository;

import com.example.restaurant.exceptions.UserNotFoundException;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {
    private final IJpaUserRepository _jpaUserRepository;
    private final IJpaRoleRepository _jpaRoleRepository;

    @Override
    public boolean existsByUsername(String username) {
        return _jpaUserRepository.findByNormalizedUsername(username).isPresent();
    }

    @Override
    public Users findByToken(String token) {
        return _jpaUserRepository.findByToken(token).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );
    }

    @Override
    public boolean isInRole(String roleName, String userToken) {
        Users user = _jpaUserRepository.findByToken(userToken).orElseThrow(() -> new UserNotFoundException("User not found"));

        Roles role = _jpaRoleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found"));

        return user.getRoles().contains(role);
    }

    @Override
    public Optional<Users> findByNormalizedUsername(String username) {
        return _jpaUserRepository.findByNormalizedUsername(username);
    }


    @Override
    public void save(Users user) {
        _jpaUserRepository.saveAndFlush(user);
    }

    @Override
    public void delete(Users user) {
        _jpaUserRepository.delete(user);
    }

    @Override
    public Optional<Users> findByNormalizedEmail(String email) {
        return _jpaUserRepository.findByNormalizedEmail(email);
    }
}
