package com.example.restaurant.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.example.restaurant.TestConstants;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.VerificationToken;
import com.example.restaurant.repository.interfaces.jpa.IJpaVerificationTokenRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationTokenRepositoryTest {
  @Mock private IJpaVerificationTokenRepository _jpaTokenRepo;

  @InjectMocks private VerificationTokenRepository _tokenRepo;

  @Test
  @DisplayName("save: Should Call Jpa Save")
  void save_ShouldCallJpaSave() {
    VerificationToken token = new VerificationToken();
    _tokenRepo.save(token);
    verify(_jpaTokenRepo, times(1)).saveAndFlush(token);
  }

  @Test
  @DisplayName("delete: Should Call Jpa")
  void delete_ShouldCallJpaDelete() {
    VerificationToken token = new VerificationToken();
    _tokenRepo.delete(token);
    verify(_jpaTokenRepo, times(1)).delete(token);
  }

  @Test
  @DisplayName("find By Token And Type: Should Call Jpa")
  void findByTokenAndType_ShouldCallJpaMethod() {
    when(_jpaTokenRepo.findByTokenAndType(
            TestConstants.FAKE_VERIFICATION_TOKEN, TokenTypeEnum.ACTIVATION))
        .thenReturn(Optional.of(new VerificationToken()));

    assertTrue(
        _tokenRepo
            .findByTokenAndType(TestConstants.FAKE_VERIFICATION_TOKEN, TokenTypeEnum.ACTIVATION)
            .isPresent());
    verify(_jpaTokenRepo, times(1))
        .findByTokenAndType(TestConstants.FAKE_VERIFICATION_TOKEN, TokenTypeEnum.ACTIVATION);
  }

  @Test
  @DisplayName("find By Token And Type: Should Return Empty When Not Found")
  void findByTokenAndType_ShouldReturnEmpty_WhenNotFound() {
    when(_jpaTokenRepo.findByTokenAndType(
            TestConstants.TOKEN_NON_EXISTENT, TokenTypeEnum.ACTIVATION))
        .thenReturn(Optional.empty());

    Optional<VerificationToken> result =
        _tokenRepo.findByTokenAndType(TestConstants.TOKEN_NON_EXISTENT, TokenTypeEnum.ACTIVATION);

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("delete By User Token And Type: Should Call Jpa")
  void deleteByUserTokenAndType_ShouldCallJpaMethod() {
    _tokenRepo.deleteByUserTokenAndType("user-token-123", TokenTypeEnum.REFRESH_TOKEN);

    verify(_jpaTokenRepo, times(1))
        .deleteByUserTokenAndType("user-token-123", TokenTypeEnum.REFRESH_TOKEN);
  }
}
