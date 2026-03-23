package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.exceptions.UserNotFoundException;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServicesTest {
    @Mock
    private IUserRepository _userRepo;

    @Mock
    private EmailServices _emailServices;

    @Mock
    private IVerificationTokenRepository _tokenRepo;

    @InjectMocks
    private UserServices _userServices;

    @Test
    void updatePassword_ShouldReturnFailure_WhenPasswordsDoNotMatch() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPass123!");
        request.setPassword("newPass123!");
        request.setConfirmPassword("diffNewPass123!");

        var result = _userServices.updatePassword(TestConstants.FAKE_USER_TOKEN, request);

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertEquals("Passwords do not match", result.getMessage());
        verify(_userRepo, never()).updatePassword(anyString(), anyString(), anyString());
    }

    @Test
    void updatePassword_ShouldThrowException_WhenOldPasswordIsInvalid() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("diffOldPass123!");
        request.setPassword("newPass123!");
        request.setConfirmPassword("newPass123!");

        doThrow(new BadCredentialsException("Invalid old password"))
                .when(_userRepo).updatePassword(TestConstants.FAKE_USER_TOKEN, "diffOldPass123!", "newPass123!");

        assertThrows(BadCredentialsException.class, () ->
                _userServices.updatePassword(TestConstants.FAKE_USER_TOKEN, request)
        );
    }

    @Test
    void updatePassword_ShouldReturnSuccess_WhenEverythingIsCorrect() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPass123!");
        request.setPassword("newPass123!");
        request.setConfirmPassword("newPass123!");


        var result = _userServices.updatePassword(
                TestConstants.FAKE_USER_TOKEN, request
        );

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Password updated", result.getMessage());
    }

    @Test
    void updatePassword_ShouldThrowUserNotFoundException() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPass123!");
        request.setPassword("newPass123!");
        request.setConfirmPassword("newPass123!");

        doThrow(new UserNotFoundException("User not found"))
                .when(_userRepo).updatePassword(anyString(), anyString(), anyString());

        assertThrows(UserNotFoundException.class, () ->
                _userServices.updatePassword(TestConstants.FAKE_USER_TOKEN, request)
        );
    }

    @Test
    void updateEmail_ShouldReturnFailure_WhenEmailIsUsedBySomeoneElse() {
        UserDomain otherUser = new UserDomain(
                "different-token",
                "otherUser",
                "OTHERUSER",
                TestConstants.FAKE_EMAIL,
                TestConstants.FAKE_EMAIL.toUpperCase()
        );

        when(_userRepo.findMinimalByEmail(TestConstants.FAKE_EMAIL))
                .thenReturn(Optional.of(otherUser));

        ResultHandler<Void> result = _userServices.updateEmail(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_EMAIL
        );

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertEquals("The email is being used by someone else", result.getMessage());
    }

    @Test
    void updateEmail_ShouldReturnSuccess_AndSendEmail_WhenValid() {
        String newEmail = "new@example.com";
        when(_userRepo.findMinimalByEmail(newEmail)).thenReturn(Optional.empty());
        when(_tokenRepo.createToken(eq(TestConstants.FAKE_USER_TOKEN), eq(TokenTypeEnum.EMAIL_UPDATE), anyInt()))
                .thenReturn("mock-verification-token");

        ResultHandler<Void> result = _userServices.updateEmail(TestConstants.FAKE_USER_TOKEN, newEmail);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());

        verify(_emailServices, times(1)).sendEmailChangeVerification(newEmail, "mock-verification-token");
    }

    @Test
    void confirmEmailChange_ShouldReturnFailure_WhenTokenIsInvalid() {
        when(_tokenRepo.validateToken(TestConstants.FAKE_USER_TOKEN, "invalid-token", TokenTypeEnum.EMAIL_UPDATE))
                .thenReturn(false);

        var result = _userServices.confirmEmailChange(TestConstants.FAKE_USER_TOKEN, "invalid-token");

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertEquals("Invalid or expired token", result.getMessage());
        verify(_userRepo, never()).confirmEmailChange(anyString());
    }

    @Test
    void confirmEmailChange_ShouldThrowException_WhenNoPendingEmail() {
        when(_tokenRepo.validateToken(TestConstants.FAKE_USER_TOKEN, "valid-token", TokenTypeEnum.EMAIL_UPDATE))
                .thenReturn(true);

        doThrow(new IllegalStateException("No pending email to confirm"))
                .when(_userRepo).confirmEmailChange(TestConstants.FAKE_USER_TOKEN);

        assertThrows(IllegalStateException.class, () ->
                _userServices.confirmEmailChange(TestConstants.FAKE_USER_TOKEN, "valid-token")
        );
    }

    @Test
    void confirmEmailChange_ShouldReturnSuccess_WhenEverythingIsValid() {
        when(_tokenRepo.validateToken(TestConstants.FAKE_USER_TOKEN, "valid-token", TokenTypeEnum.EMAIL_UPDATE))
                .thenReturn(true);

        ResultHandler<Void> result = _userServices.confirmEmailChange(TestConstants.FAKE_USER_TOKEN, "valid-token");

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Email updated successfully", result.getMessage());
    }

    @Test
    void updateUserName_ShouldReturnFailure_WhenUsernameExists() {
        String newName = "existingUser";
        when(_userRepo.existsByUsername(newName)).thenReturn(true);

        ResultHandler<Void> result = _userServices.updateUserName(newName, TestConstants.FAKE_USER_TOKEN);

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertEquals("Username is already taken", result.getMessage());
    }

    @Test
    void updateUserName_ShouldReturnSuccess_WhenEverythingIsValid() {
        String newName = "newName";
        when(_userRepo.existsByUsername(newName)).thenReturn(false);

        ResultHandler<Void> result = _userServices.updateUserName(newName, TestConstants.FAKE_USER_TOKEN);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("User name changed successfully", result.getMessage());
    }

    @Test
    void deleteAccount_ShouldReturnSuccess_WhenUserExistsAndDeleted() {
        ResultHandler<Void> result = _userServices.deleteAccount(TestConstants.FAKE_USER_TOKEN);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("User deleted successfully", result.getMessage());
        verify(_userRepo, times(1)).delete(TestConstants.FAKE_USER_TOKEN);
    }
}
