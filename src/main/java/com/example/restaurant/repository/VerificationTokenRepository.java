package com.example.restaurant.repository;

import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.VerificationToken;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VerificationTokenRepository implements IVerificationTokenRepository {
    private final IJpaVerificationTokenRepository _jpaTokenRepo;
    private final IJpaUserRepository _jpaUserRepo;

    @Override
    public void save(VerificationToken token) {
        _jpaTokenRepo.saveAndFlush(token);
    }

    @Override
    public void delete(VerificationToken token) {
        _jpaTokenRepo.delete(token);
    }

    @Override
    public Optional<VerificationToken> findByTokenAndType(String tokenValue, TokenTypeEnum type) {
        return _jpaTokenRepo.findByTokenAndType(tokenValue, type);
    }

    @Override
    public void deleteByUserTokenAndType(String userToken, TokenTypeEnum type) {
        _jpaTokenRepo.deleteByUserTokenAndType(userToken, type);
    }
}
