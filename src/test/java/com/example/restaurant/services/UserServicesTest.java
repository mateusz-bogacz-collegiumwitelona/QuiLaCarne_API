package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServicesTest {
    @Mock
    private IUserRepository _userRepo;
    @Mock
    private EmailServices _emailServices;
    @Mock
    private IVerificationTokenRepository _tokenRepo;
    @Mock
    private IRoleRepository _roleRepository;
    @Mock
    private PasswordEncoder _passwordEncoder;

    @InjectMocks
    private UserServices _userServices;

    @Test
    void create_ShouldHashPasswordAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@test.pl");
        request.setPassword("password");

        when(_roleRepository.setRole(anyString())).thenReturn(new Roles());
        when(_passwordEncoder.encode("password")).thenReturn("hashedPass");

        doAnswer(invocation -> {
            Users savedUser = invocation.getArgument(0);
            savedUser.setToken("generated-mock-token");
            return null;
        }).when(_userRepo).save(any(Users.class));

        String token = _userServices.create(request, "ROLE_CLIENT", true);

        assertNotNull(token);
        assertEquals("generated-mock-token", token);
        verify(_userRepo, times(1)).save(any(Users.class));
    }

    @Test
    void updatePassword_ShouldReturnFailure_WhenPasswordsDoNotMatch() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPass123!");
        request.setPassword("newPass123!");
        request.setConfirmPassword("diffNewPass123!");

        var result = _userServices.updatePassword(TestConstants.FAKE_USER_TOKEN, request);

        assertFalse(result.isSuccess());
        verify(_userRepo, never()).save(any());
    }

    @Test
    void updatePassword_ShouldThrowException_WhenOldPasswordIsInvalid() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("wrongOldPass");
        request.setPassword("newPass123!");
        request.setConfirmPassword("newPass123!");

        Users mockUser = new Users();
        mockUser.setPassword("hashedOld");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(mockUser);
        when(_passwordEncoder.matches("wrongOldPass", "hashedOld")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () ->
                _userServices.updatePassword(TestConstants.FAKE_USER_TOKEN, request)
        );
    }

    @Test
    void updatePassword_ShouldReturnSuccess_WhenEverythingIsCorrect() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("correctOldPass");
        request.setPassword("newPass123!");
        request.setConfirmPassword("newPass123!");

        Users mockUser = new Users();
        mockUser.setPassword("hashedOld");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(mockUser);
        when(_passwordEncoder.matches("correctOldPass", "hashedOld")).thenReturn(true);
        when(_passwordEncoder.encode("newPass123!")).thenReturn("hashedNew");

        var result = _userServices.updatePassword(TestConstants.FAKE_USER_TOKEN, request);

        assertTrue(result.isSuccess());
        assertEquals("hashedNew", mockUser.getPassword());
        verify(_userRepo, times(1)).save(mockUser);
    }

    @Test
    void updateEmail_ShouldReturnFailure_WhenEmailIsUsedBySomeoneElse() {
        Users otherUser = new Users();
        otherUser.setToken("different-token");

        when(_userRepo.findByNormalizedEmail(anyString())).thenReturn(Optional.of(otherUser));

        ResultHandler<Void> result = _userServices.updateEmail(TestConstants.FAKE_USER_TOKEN, TestConstants.FAKE_EMAIL);

        assertFalse(result.isSuccess());
        assertEquals("The email is being used by someone else", result.getMessage());
    }

    @Test
    void updateEmail_ShouldReturnSuccess_AndSendEmail_WhenValid() {
        when(_userRepo.findByNormalizedEmail(anyString())).thenReturn(Optional.empty());
        Users currentUser = new Users();

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(currentUser);
        when(_tokenRepo.createToken(anyString(), eq(TokenTypeEnum.EMAIL_UPDATE), anyInt()))
                .thenReturn("mock-token");

        var result = _userServices.updateEmail(TestConstants.FAKE_USER_TOKEN, "new@test.pl");

        assertTrue(result.isSuccess());
        assertEquals("new@test.pl", currentUser.getPendingEmail());
        verify(_userRepo).save(currentUser);
        verify(_emailServices).sendEmailChangeVerification("new@test.pl", "mock-token");
    }

    @Test
    void confirmEmailChange_ShouldReturnSuccess_WhenEverythingIsValid() {
        when(_tokenRepo.validateToken(anyString(), anyString(), eq(TokenTypeEnum.EMAIL_UPDATE))).thenReturn(true);

        Users mockUser = new Users();
        mockUser.setPendingEmail("new@test.pl");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(mockUser);

        var result = _userServices.confirmEmailChange(TestConstants.FAKE_USER_TOKEN, "valid-token");

        assertTrue(result.isSuccess());
        assertEquals("new@test.pl", mockUser.getEmail());
        assertNull(mockUser.getPendingEmail());
        verify(_userRepo).save(mockUser);
    }

    @Test
    void deleteAccount_ShouldReturnSuccess_AndAnonymizeData() {
        Users mockUser = new Users();
        mockUser.setNormalizedEmail("TEST@TEST.PL");
        mockUser.setNormalizedUsername("TEST");
        mockUser.setEmail("test@test.pl");
        mockUser.setUsername("test");
        mockUser.setIsActive(true);

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(mockUser);

        var result = _userServices.deleteAccount(TestConstants.FAKE_USER_TOKEN);

        assertTrue(result.isSuccess());
        assertFalse(mockUser.getIsActive());
        assertTrue(mockUser.getNormalizedEmail().startsWith("DELETED_"));
        verify(_userRepo, times(1)).save(mockUser);
    }
}