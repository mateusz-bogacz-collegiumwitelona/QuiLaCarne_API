package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.exceptions.UserNotFoundException;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserRepositoryTest {
    @InjectMocks
    private UserRepository _userRepository;

    @Mock
    private IJpaUserRepository _jpaUserRepository;

    @Mock
    private IJpaRoleRepository _jpaRoleRepository;

    @Test
    void existsByUsername_ShouldReturnTrue_WhenUserExists() {
        when(_jpaUserRepository.findByNormalizedUsername(anyString())).thenReturn(Optional.of(new Users()));
        assertTrue(_userRepository.existsByUsername("TEST"));
    }

    @Test
    void findByToken_ShouldReturnUser_WhenFound() {
        Users mockUser = new Users();
        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(Optional.of(mockUser));

        Users result = _userRepository.findByToken(TestConstants.FAKE_USER_TOKEN);
        assertEquals(mockUser, result);
    }

    @Test
    void findByToken_ShouldThrowException_WhenNotFound() {
        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                _userRepository.findByToken(TestConstants.FAKE_USER_TOKEN)
        );
    }

    @Test
    void isInRole_ShouldReturnTrue_WhenUserHasThisRole() {
        Roles targetRole = new Roles();
        targetRole.setName(TestConstants.FAKE_ROLE);

        Users mockUser = new Users();
        mockUser.setRoles(Set.of(targetRole));

        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(Optional.of(mockUser));
        when(_jpaRoleRepository.findByName(TestConstants.FAKE_ROLE)).thenReturn(Optional.of(targetRole));

        assertTrue(_userRepository.isInRole(TestConstants.FAKE_ROLE, TestConstants.FAKE_USER_TOKEN));
    }

    @Test
    void save_ShouldCallJpaSave() {
        Users user = new Users();
        _userRepository.save(user);
        verify(_jpaUserRepository, times(1)).saveAndFlush(user);
    }

    @Test
    void delete_ShouldCallJpaDelete() {
        Users user = new Users();
        _userRepository.delete(user);
        verify(_jpaUserRepository, times(1)).delete(user);
    }

    @Test
    void findByNormalizedEmail_ShouldCallJpa() {
        when(_jpaUserRepository.findByNormalizedEmail(anyString())).thenReturn(Optional.of(new Users()));
        assertTrue(_userRepository.findByNormalizedEmail("TEST@TEST.PL").isPresent());
    }
}