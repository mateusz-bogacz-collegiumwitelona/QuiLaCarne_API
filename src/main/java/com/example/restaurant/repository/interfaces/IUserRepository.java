package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.Users;

import java.util.Optional;

public interface IUserRepository {
    boolean existsByUsername(String username);

    boolean existByEmail(String email);

    boolean isInRole(String roleToken, String userToken);

    Users findByToken(String token);

    Optional<Users> findByNormalizedUsername(String username);

    void save(Users user);

    void delete(Users user);

    Optional<Users> findByNormalizedEmail(String email);
}
