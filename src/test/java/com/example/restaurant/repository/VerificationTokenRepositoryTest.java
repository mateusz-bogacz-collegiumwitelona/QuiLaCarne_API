package com.example.restaurant.repository;

import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.VerificationToken;
import com.example.restaurant.repository.interfaces.jpa.IJpaVerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VerificationTokenRepositoryTest {
    @Mock
    private IJpaVerificationTokenRepository _jpaTokenRepo;

    @InjectMocks
    private VerificationTokenRepository _tokenRepo;

    @Test
    void save_ShouldCallJpaSave() {
        VerificationToken token = new VerificationToken();
        _tokenRepo.save(token);
        verify(_jpaTokenRepo, times(1)).saveAndFlush(token);
    }

    @Test
    void delete_ShouldCallJpaDelete() {
        VerificationToken token = new VerificationToken();
        _tokenRepo.delete(token);
        verify(_jpaTokenRepo, times(1)).delete(token);
    }

    @Test
    void findByTokenAndType_ShouldCallJpaMethod() {
        when(_jpaTokenRepo.findByTokenAndType("token", TokenTypeEnum.ACTIVATION))
                .thenReturn(Optional.of(new VerificationToken()));

        assertTrue(_tokenRepo.findByTokenAndType("token", TokenTypeEnum.ACTIVATION).isPresent());
        verify(_jpaTokenRepo, times(1)).findByTokenAndType("token", TokenTypeEnum.ACTIVATION);
    }
}