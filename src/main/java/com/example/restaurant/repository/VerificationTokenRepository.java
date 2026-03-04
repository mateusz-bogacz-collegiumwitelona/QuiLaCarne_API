package com.example.restaurant.repository;

import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.VerificationToken;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaVerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
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
    public boolean activeUser(String token) {
        return _jpaTokenRepo.findByTokenAndType(token, TokenTypeEnum.ACTIVATION)
                .map(vt -> {
                    if (vt.isExpired()) return false;

                    Users user = vt.getUser();
                    user.setIsActive(true);

                    _jpaUserRepo.saveAndFlush(user);

                    _jpaTokenRepo.delete(vt);

                    return true;
                }).orElse(false);
    }

    @Override
    @Transactional
    public boolean resetUserPassowrd(String verificationTokenValue, String newPassword) {
        return _jpaTokenRepo.findByTokenAndType(verificationTokenValue, TokenTypeEnum.PASSWORD_RESET)
                .map(vt -> {
                    if (vt.isExpired()) return false;

                    Users user = vt.getUser();

                    boolean isChanged = _userRepo.changePassword(user.getToken(), newPassword);

                    if (isChanged) {
                        _jpaTokenRepo.delete(vt);
                        return true;
                    }

                    return false;
                }).orElse(false);
    }
}
