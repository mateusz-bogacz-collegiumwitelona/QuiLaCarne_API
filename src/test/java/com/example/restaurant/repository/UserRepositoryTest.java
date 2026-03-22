package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.exceptions.UserNotFoundException;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserRepositoryTest {
    @InjectMocks
    private UserRepository _userRepository;

    @Mock
    private IJpaUserRepository _jpaUserRepository;

    @Mock
    private IRoleRepository _roleRepository;

    @Mock
    private PasswordEncoder _passwordEncoder;

    @Mock
    private IJpaRoleRepository _jpaRoleRepository;

    @Test
    void createUser_ShouldHashPasswordAndRetrunToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(TestConstants.FAKE_USERNAME);
        request.setEmail(TestConstants.FAKE_EMAIL);
        request.setPassword(TestConstants.FAKE_PASSWORD);

        Roles mockRole = new Roles();
        mockRole.setName("ROLE_CLIENT");

        Users mockUser = new Users();
        mockUser.setToken(TestConstants.FAKE_USER_TOKEN);

        when(_roleRepository.setRole("ROLE_CLIENT")).thenReturn(mockRole);
        when(_passwordEncoder.encode(TestConstants.FAKE_PASSWORD)).thenReturn(TestConstants.FAKE_HASH);

        when(_jpaUserRepository.saveAndFlush(any(Users.class))).thenReturn(mockUser);

        String token = _userRepository.createUser(request, "ROLE_CLIENT", false);

        assertEquals(TestConstants.FAKE_USER_TOKEN, token);
        verify(_jpaUserRepository).saveAndFlush(argThat(user ->
                user.getNormalizedUsername().equals(TestConstants.FAKE_USERNAME.toUpperCase()) &&
                        user.getNormalizedEmail().equals(TestConstants.FAKE_EMAIL.toUpperCase())
        ));
    }

    @Test
    void findMinimalByEmail_ShouldReturnDTO_WhenUserIsExist() {
        String email = TestConstants.FAKE_EMAIL;
        String normalizedEmail = email.toUpperCase().trim();

        Users user = new Users();
        user.setToken(TestConstants.FAKE_USER_TOKEN);
        user.setUsername(TestConstants.FAKE_USERNAME);
        user.setNormalizedUsername(TestConstants.FAKE_USERNAME.toUpperCase());
        user.setEmail(email);
        user.setNormalizedEmail(normalizedEmail);

        when(_jpaUserRepository.findByNormalizedEmail(normalizedEmail))
                .thenReturn(Optional.of(user));

        var result = _userRepository.findMinimalByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(TestConstants.FAKE_USER_TOKEN, result.get().token());
        assertEquals(normalizedEmail, result.get().normalizedEmail());
    }

    @Test
    void changePassword_ShouldReturnHashAndSaveUser() {
        Users user = new Users();
        user.setToken(TestConstants.FAKE_USER_TOKEN);

        when(_jpaUserRepository
                .findByToken(TestConstants.FAKE_USER_TOKEN)
        ).thenReturn(Optional.of(user));

        when(_passwordEncoder
                .encode(TestConstants.FAKE_PASSWORD)
        ).thenReturn(TestConstants.FAKE_HASH);

        boolean result = _userRepository
                .changePassword(
                        TestConstants.FAKE_USER_TOKEN,
                        TestConstants.FAKE_PASSWORD
                );

        assertTrue(result);

        assertEquals(
                TestConstants.FAKE_HASH,
                user.getPassword()
        );

        verify(_jpaUserRepository).saveAndFlush(user);
    }

    @Test
    void updatePassword_ShouldThrowException_WhenUserNotFound() {
        when(_jpaUserRepository.findByToken(anyString()))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                _userRepository.updatePassword(
                        TestConstants.FAKE_USER_TOKEN,
                        "oldPass",
                        "newPass"
                )
        );
    }

    @Test
    void updatePassword_ShouldReturnFalse_WhenOldPasswordIsIncorrect() {
        Users mockUser = new Users();
        mockUser.setPassword(TestConstants.FAKE_HASH);

        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN))
                .thenReturn(Optional.of(mockUser));

        when(_passwordEncoder.matches("wrongOldPass", TestConstants.FAKE_HASH))
                .thenReturn(false);

        boolean result = _userRepository.updatePassword(
                TestConstants.FAKE_USER_TOKEN,
                "wrongOldPass",
                "newPass"
        );

        assertFalse(result);
        verify(_jpaUserRepository, never()).saveAndFlush(mockUser);
    }

    @Test
    void updatePassword_ShouldReturnTrueAndSave_WhenOldPasswordIsCorrect() {
        Users mockUser = new Users();
        mockUser.setPassword("hashedOldPass");

        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(Optional.of(mockUser));

        when(_passwordEncoder.matches("correctOldPass", "hashedOldPass")).thenReturn(true);
        when(_passwordEncoder.encode("newPass")).thenReturn("hashedNewPass");

        boolean result = _userRepository.updatePassword(TestConstants.FAKE_USER_TOKEN, "correctOldPass", "newPass");

        assertTrue(result);
        assertEquals("hashedNewPass", mockUser.getPassword());
        verify(_jpaUserRepository, times(1)).saveAndFlush(mockUser);
    }

    @Test
    void activeUser_ShouldReturnTrueAndSetActive_WhenUserExists() {
        Users mockUser = new Users();
        mockUser.setIsActive(false);

        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN))
                .thenReturn(Optional.of(mockUser));

        boolean result = _userRepository.activeUser(TestConstants.FAKE_USER_TOKEN);

        assertTrue(result);
        assertTrue(mockUser.getIsActive());
        verify(_jpaUserRepository, times(1)).saveAndFlush(mockUser);
    }

    @Test
    void activeUser_ShouldReturnFalse_WhenUserNotFound() {
        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                _userRepository.activeUser(TestConstants.FAKE_USER_TOKEN)
        );
    }

    @Test
    void updateEmail_ShouldSetPendingEmailAndSave_WhenUserExists() {
        Users mockUser = new Users();
        mockUser.setEmail("old@example.com");

        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN))
                .thenReturn(Optional.of(mockUser));

        boolean result = _userRepository.updateEmail(TestConstants.FAKE_USER_TOKEN, "new@example.com");

        assertTrue(result);
        assertEquals("new@example.com", mockUser.getPendingEmail());
        assertEquals("old@example.com", mockUser.getEmail());
        verify(_jpaUserRepository, times(1)).saveAndFlush(mockUser);
    }

    @Test
    void updateEmail_ShouldThrowException_WhenUserNotFound() {
        when(_jpaUserRepository.findByToken(anyString()))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                _userRepository.updateEmail("invalid-token", "new@example.com")
        );
    }

    @Test
    void confirmEmailChange_ShouldUpdateEmailAndClearPending_WhenValid() {
        Users mockUser = new Users();
        mockUser.setEmail("old@example.com");
        mockUser.setPendingEmail("new@example.com");

        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN))
                .thenReturn(Optional.of(mockUser));

        boolean result = _userRepository.confirmEmailChange(TestConstants.FAKE_USER_TOKEN);

        assertTrue(result);
        assertEquals("new@example.com", mockUser.getEmail());
        assertEquals("NEW@EXAMPLE.COM", mockUser.getNormalizedEmail());
        assertNull(mockUser.getPendingEmail());
        verify(_jpaUserRepository, times(1)).saveAndFlush(mockUser);
    }

    @Test
    void confirmEmailChange_ShouldReturnFalse_WhenPendingEmailIsNull() {
        Users mockUser = new Users();
        mockUser.setPendingEmail(null);

        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN))
                .thenReturn(Optional.of(mockUser));

        boolean result = _userRepository.confirmEmailChange(TestConstants.FAKE_USER_TOKEN);

        assertFalse(result);
        verify(_jpaUserRepository, never()).saveAndFlush(any());
    }

    @Test
    void changeUserName_ShouldReturnTrue_AndSave_WhenUserExists() {
        Users mockUser = new Users();
        mockUser.setUsername(TestConstants.FAKE_USERNAME);
        mockUser.setNormalizedUsername(TestConstants.FAKE_USERNAME.toLowerCase().trim());
        mockUser.setIsActive(true);
        mockUser.setToken(TestConstants.FAKE_USER_TOKEN);

        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN))
                .thenReturn(Optional.of(mockUser));

        boolean resutl = _userRepository.changeUserName(TestConstants.FAKE_USER_TOKEN, "user12");

        assertTrue(resutl);
        assertEquals("user12", mockUser.getUsername());
        assertEquals("USER12", mockUser.getNormalizedUsername());

        verify(_jpaUserRepository, times(1)).saveAndFlush(mockUser);
    }

    @Test
    void changeUserName_ShouldThrowException_WhenUserNotFound() {
        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                _userRepository.changeUserName(TestConstants.FAKE_USER_TOKEN, "user12")
        );
    }

    @Test
    void deleteUser_ShouldReturnTrue_WhenUserExists() {
        Users mockUser = new Users();
        mockUser.setUsername(TestConstants.FAKE_USERNAME);
        mockUser.setNormalizedUsername(TestConstants.FAKE_USERNAME.toLowerCase().trim());
        mockUser.setToken(TestConstants.FAKE_USER_TOKEN);

        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(Optional.of(mockUser));

        boolean resutl = _userRepository.delete(TestConstants.FAKE_USER_TOKEN);

        assertTrue(resutl);
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserNotFound() {
        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                _userRepository.delete(TestConstants.FAKE_USER_TOKEN)
        );
    }

    @Test
    void isInRole_ShouldReturnTrue_WhenUserHasThisRole() {
        Roles targetRole = new Roles();
        targetRole.setName(TestConstants.FAKE_ROLE);

        Users mockUser = new Users();
        mockUser.setToken(TestConstants.FAKE_USER_TOKEN);
        mockUser.setRoles(Set.of(targetRole));

        when(_jpaUserRepository.findByToken(
                TestConstants.FAKE_USER_TOKEN
        )).thenReturn(Optional.of(mockUser));
        when(_jpaRoleRepository.findByName(
                TestConstants.FAKE_ROLE
        )).thenReturn(Optional.of(targetRole));

        boolean result = _userRepository.isInRole(
                TestConstants.FAKE_ROLE,
                TestConstants.FAKE_USER_TOKEN
        );

        assertTrue(result);
    }

    @Test
    void isInRole_ShouldReturnFalse_WhenUserDoesNotHaveThisRole() {
        Roles userRole = new Roles();
        userRole.setName(TestConstants.FAKE_ROLE);

        Roles searchedRole = new Roles();
        searchedRole.setName(
                TestConstants.FAKE_ROLE + TestConstants.FAKE_ROLE
        );

        Users mockUser = new Users();
        mockUser.setToken(TestConstants.FAKE_USER_TOKEN);
        mockUser.setRoles(Set.of(userRole));

        when(_jpaUserRepository.findByToken(
                TestConstants.FAKE_USER_TOKEN
        )).thenReturn(Optional.of(mockUser));
        when(_jpaRoleRepository.findByName(
                TestConstants.FAKE_ROLE)
        ).thenReturn(Optional.of(searchedRole));

        boolean result = _userRepository.isInRole(TestConstants.FAKE_ROLE, TestConstants.FAKE_USER_TOKEN);

        assertFalse(result);
    }
}
