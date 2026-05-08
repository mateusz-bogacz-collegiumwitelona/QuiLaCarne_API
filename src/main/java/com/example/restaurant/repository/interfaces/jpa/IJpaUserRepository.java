package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Users;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IJpaUserRepository
    extends JpaRepository<Users, UUID>, JpaSpecificationExecutor<Users> {
  Optional<Users> findByUsername(String username);

  Optional<Users> findByNormalizedUsername(String normalizedUsername);

  Optional<Users> findByToken(String token);

  Optional<Users> findByEmail(String email);

  Optional<Users> findByNormalizedEmail(String normalizedEmail);

  Optional<Users> findByUsernameOrEmail(String username, String email);
}
