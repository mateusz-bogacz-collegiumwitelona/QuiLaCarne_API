package com.example.restaurant.services;

import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.VerificationToken;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
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
public class VerificationTokenServicesTest {
    @Mock
    private IVerificationTokenRepository _tokenRepo;

    @Mock
    private IUserRepository _userRepo;

    @InjectMocks
    private VerificationTokenServices _tokenServices;

    @Test
    void createToken_ShouldSaveAndReturnTokenValue() {
        Users mockUser = new Users();
        when(_userRepo.findByToken("user-token")).thenReturn(mockUser);

        String tokenValue = _tokenServices.createToken("user-token", TokenTypeEnum.ACTIVATION, 15);

        assertNotNull(tokenValue);
        verify(_tokenRepo, times(1)).save(any(VerificationToken.class));
    }

    @Test
    void validateToken_WithUserToken_ShouldReturnTrueAndDelete_WhenValid() {
        Users mockUser = new Users();
        mockUser.setToken("user-token-123");

        VerificationToken vt = new VerificationToken();
        vt.setUser(mockUser);
        vt.setExpiryDate(OffsetDateTime.now().plusMinutes(15));

        when(_tokenRepo.findByTokenAndType("valid-token", TokenTypeEnum.ACTIVATION))
                .thenReturn(Optional.of(vt));

        boolean result = _tokenServices.validateToken("user-token-123", "valid-token", TokenTypeEnum.ACTIVATION);

        assertTrue(result);
        verify(_tokenRepo, times(1)).delete(vt);
    }

    @Test
    void validateToken_WithoutUserToken_ShouldReturnUserTokenAndDelete_WhenValid() {
        Users mockUser = new Users();
        mockUser.setToken("user-token-123");

        VerificationToken vt = new VerificationToken();
        vt.setUser(mockUser);
        vt.setExpiryDate(OffsetDateTime.now().plusMinutes(15));

        when(_tokenRepo.findByTokenAndType("valid-token", TokenTypeEnum.ACTIVATION))
                .thenReturn(Optional.of(vt));

        Optional<String> result = _tokenServices.validateToken("valid-token", TokenTypeEnum.ACTIVATION);

        assertTrue(result.isPresent());
        assertEquals("user-token-123", result.get());
        verify(_tokenRepo, times(1)).delete(vt);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenExpired() {
        VerificationToken vt = new VerificationToken();
        vt.setExpiryDate(OffsetDateTime.now().minusMinutes(15));

        when(_tokenRepo.findByTokenAndType("expired-token", TokenTypeEnum.ACTIVATION))
                .thenReturn(Optional.of(vt));

        boolean result = _tokenServices.validateToken("user-token-123", "expired-token", TokenTypeEnum.ACTIVATION);

        assertFalse(result);
        verify(_tokenRepo, never()).delete(any());
    }
}