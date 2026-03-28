package com.example.restaurant.services;

import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.VerificationToken;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenServices implements IVerificationTokenServices {
    private final IVerificationTokenRepository _tokenRepo;
    private final IUserRepository _userRepo;

    @Override
    @Transactional
    public String createToken(String userToken, TokenTypeEnum type, int expiryMinutes) {
        var user = _userRepo.findByToken(userToken);

        String tokenValue = UUID.randomUUID().toString();

        VerificationToken vt = new VerificationToken();
        vt.setToken(tokenValue);
        vt.setUser(user);
        vt.setType(type);
        vt.setExpiryDate(OffsetDateTime.now().plusMinutes(expiryMinutes));
        vt.setCreatedAt(OffsetDateTime.now());

        _tokenRepo.save(vt);
        return tokenValue;
    }

    @Override
    @Transactional
    public boolean validateToken(String userToken, String tokenValue, TokenTypeEnum type) {
        return _tokenRepo.findByTokenAndType(tokenValue, type)
                .map(vt -> {
                    if (vt.isExpired()) return false;
                    if (!vt.getUser().getToken().equals(userToken)) return false;

                    _tokenRepo.delete(vt);
                    return true;
                }).orElse(false);
    }

    @Override
    @Transactional
    public Optional<String> validateToken(String tokenValue, TokenTypeEnum type) {
        return _tokenRepo.findByTokenAndType(tokenValue, type)
                .filter(vt -> !vt.isExpired())
                .map(vt -> {
                    String userToken = vt.getUser().getToken();
                    _tokenRepo.delete(vt);
                    return userToken;
                });
    }

}
