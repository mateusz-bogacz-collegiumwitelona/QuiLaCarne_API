package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.VerificationToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IJpaVerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
  Optional<VerificationToken> findByTokenAndType(String token, TokenTypeEnum type);

  void deleteByUserTokenAndType(String userToken, TokenTypeEnum type);
}
