package com.example.restaurant.repository;

import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.VerificationToken;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaVerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VerificationTokenRepositoryTest {
    @Mock
    private IJpaVerificationTokenRepository _jpaTokenRepo;

    @Mock
    private IJpaUserRepository _jpaUserRepo;

    @Mock
    private IUserRepository _userRepo;

    @InjectMocks
    private VerificationTokenRepository _tokenRepo;

    @Test
    void validateToken_ShouldReturnUserTokenAndDelete_WhenValid() {
        Users mockUser = new Users();
        mockUser.setToken("user-token-123");

        VerificationToken vt = new VerificationToken();
        vt.setUser(mockUser);
        vt.setExpiryDate(OffsetDateTime.now().plusMinutes(15));

        when(_jpaTokenRepo.findByTokenAndType("valid-token", TokenTypeEnum.ACTIVATION))
                .thenReturn(Optional.of(vt));

        Optional<String> result = _tokenRepo.validateToken("valid-token", TokenTypeEnum.ACTIVATION);

        assertTrue(result.isPresent());
        assertEquals("user-token-123", result.get());
        verify(_jpaTokenRepo, times(1)).delete(vt);
    }

    @Test
    void validateToken_ShouldReturnEmpty_WhenTokenExpired() {

        VerificationToken vt = new VerificationToken();
        vt.setExpiryDate(OffsetDateTime.now().minusMinutes(15));

        when(_jpaTokenRepo.findByTokenAndType("expired-token", TokenTypeEnum.ACTIVATION))
                .thenReturn(Optional.of(vt));

        Optional<String> result = _tokenRepo.validateToken("expired-token", TokenTypeEnum.ACTIVATION);

        assertFalse(result.isPresent());
        verify(_jpaTokenRepo, never()).delete(any());
    }

    @Test
    void validateToken_ShouldReturnEmpty_WhenNotFound() {
        when(_jpaTokenRepo.findByTokenAndType(anyString(), any()))
                .thenReturn(Optional.empty());

        Optional<String> result = _tokenRepo.validateToken("invalid-token", TokenTypeEnum.ACTIVATION);

        assertFalse(result.isPresent());
    }
}
