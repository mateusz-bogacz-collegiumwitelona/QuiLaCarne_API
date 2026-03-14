package com.example.restaurant.repository;

import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaVerificationTokenRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

//    @Test
//    void activeUser_ShouldRetrunFalse_WhenTokenIsExpired() {
//        VerificationToken vt = new VerificationToken();
//        vt.setExpiryDate(OffsetDateTime.now().minusMinutes(5));
//
//        when(_jpaTokenRepo
//                .findByTokenAndType(
//                        TestConstants.FAKE_USER_TOKEN,
//                        TokenTypeEnum.ACTIVATION)
//        ).thenReturn(Optional.of(vt));
//
//        boolean result = _tokenRepo.activeUser(TestConstants.FAKE_USER_TOKEN);
//
//        assertFalse(result);
//        verify(_jpaUserRepo, never()).save(any());
//    }

//    @Test
//    void resetUserPassowrd_ShouldSuccessed_AndDeleteToken() {
//        Users user = new Users();
//        user.setToken(TestConstants.FAKE_USER_TOKEN);
//
//        VerificationToken vt = new VerificationToken();
//        vt.setUser(user);
//        vt.setExpiryDate(OffsetDateTime.now().plusMinutes(15));
//
//        when(_jpaTokenRepo.findByTokenAndType(TestConstants.FAKE_USER_TOKEN, TokenTypeEnum.PASSWORD_RESET))
//                .thenReturn(Optional.of(vt));
//
//        when(_userRepo.changePassword(TestConstants.FAKE_USER_TOKEN, TestConstants.FAKE_PASSWORD))
//                .thenReturn(true);
//
//        boolean result = _tokenRepo.resetUserPassowrd(TestConstants.FAKE_USER_TOKEN, TestConstants.FAKE_PASSWORD);
//
//        assertTrue(result);
//        verify(_jpaTokenRepo).delete(vt);
//    }
}
