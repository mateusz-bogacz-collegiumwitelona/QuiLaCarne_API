package com.example.restaurant.repository;

import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.VerificationToken;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaVerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VerificationTokenRepository implements IVerificationTokenRepository {
    private final IJpaVerificationTokenRepository _jpaTokenRepo;
    private final IJpaUserRepository _jpaUserRepo;
    private final IUserRepository _userRepo;

    @Override
    @Transactional
    public String createToken(String userToken, TokenTypeEnum type, int expiryMinutes) {
        var user = _jpaUserRepo.findByToken(userToken)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String tokenValue = UUID.randomUUID().toString();

        VerificationToken vt = new VerificationToken();
        vt.setToken(tokenValue);
        vt.setUser(user);
        vt.setType(type);
        vt.setExpiryDate(OffsetDateTime.now().plusMinutes(expiryMinutes));
        vt.setCreatedAt(OffsetDateTime.now());

        _jpaTokenRepo.saveAndFlush(vt);
        return tokenValue;
    }

    @Override
    @Transactional
    public boolean validateToken(String userToken, String tokenValue, TokenTypeEnum type) {
        return _jpaTokenRepo.findByTokenAndType(tokenValue, type)
                .map(vt -> {
                    if (vt.isExpired()) return false;
                    if (!vt.getUser().getToken().equals(userToken)) return false;

                    _jpaTokenRepo.delete(vt);

                    return true;
                }).orElse(false);
    }

    @Override
    @Transactional
    public Optional<String> validateToken(String tokenValue, TokenTypeEnum type) {
        return _jpaTokenRepo.findByTokenAndType(tokenValue, type)
                .filter(vt -> !vt.isExpired())
                .map(vt -> {
                    String userToken = vt.getUser().getToken();
                    _jpaTokenRepo.delete(vt);
                    return userToken;
                });
    }
}
