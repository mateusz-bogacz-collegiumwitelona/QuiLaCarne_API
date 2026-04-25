package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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

    Page<Users> findAllUsers(Specification<Users> spec, Pageable pageable);

    long count();
}
