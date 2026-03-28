package com.example.restaurant.repository.interfaces;

import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.VerificationToken;

import java.util.Optional;

public interface IVerificationTokenRepository {
    void save(VerificationToken token);

    void delete(VerificationToken token);

    Optional<VerificationToken> findByTokenAndType(String tokenValue, TokenTypeEnum type);
}
