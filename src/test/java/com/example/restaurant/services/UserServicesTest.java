package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServicesTest {
    @Mock
    private IUserRepository _userRepo;

    @InjectMocks
    private UserServices _userServices;

    @Test
    void updatePassword_ShouldReturnFailure_WhenPasswordsDoNotMatch() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPass123!");
        request.setPassword("newPass123!");
        request.setConfirmPassword("diffNewPass123!");

        ResultHandler<String> result = _userServices.updatePassword(
                TestConstants.FAKE_USER_TOKEN, request
        );

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertEquals("Passwords do not match", result.getMessage());
        verify(_userRepo, never()).updatePassword(anyString(), anyString(), anyString());
    }

    @Test
    void updatePassword_ShouldReturnFailure_WhenOldPasswordIsInvalid() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("diffOldPass123!");
        request.setPassword("newPass123!");
        request.setConfirmPassword("newPass123!");

        when(_userRepo.updatePassword(
                TestConstants.FAKE_USER_TOKEN,
                "diffOldPass123!",
                "newPass123!"
        )).thenReturn(false);

        ResultHandler<String> result = _userServices.updatePassword(
                TestConstants.FAKE_USER_TOKEN, request
        );

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCode());
        assertEquals("Invalid old Password", result.getMessage());
    }

    @Test
    void updatePassword_ShouldReturnSuccess_WhenEverythingIsCorrect() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPass123!");
        request.setPassword("newPass123!");
        request.setConfirmPassword("newPass123!");

        when(_userRepo.updatePassword(
                TestConstants.FAKE_USER_TOKEN,
                "oldPass123!",
                "newPass123!"
        )).thenReturn(true);

        ResultHandler<String> result = _userServices.updatePassword(
                TestConstants.FAKE_USER_TOKEN, request
        );

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Password updated", result.getMessage());
    }

    @Test
    void updatePassword_ShouldReturnNotFound_WhenUserNotFound() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPass123!");
        request.setPassword("newPass123!");
        request.setConfirmPassword("newPass123!");

        when(_userRepo.updatePassword(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("User not found"));

        ResultHandler<String> result = _userServices.updatePassword(
                TestConstants.FAKE_USER_TOKEN, request
        );

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        assertEquals("User not found", result.getMessage());
    }
}
