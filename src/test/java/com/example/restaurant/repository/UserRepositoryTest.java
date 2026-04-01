package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
    @DisplayName("exists By Username: should return true when user exists")
    void existsByUsername_ShouldReturnTrue_WhenUserExists() {
        when(_jpaUserRepository.findByNormalizedUsername(anyString())).thenReturn(Optional.of(new Users()));
        assertTrue(_userRepository.existsByUsername("TEST"));
    }


    @Test
    @DisplayName("find By Normalized Username: Should Call Jpa")
    void findByNormalizedUsername_ShouldCallJpa() {
        when(_jpaUserRepository.findByNormalizedUsername(anyString())).thenReturn(Optional.of(new Users()));
        assertTrue(_userRepository.findByNormalizedUsername("TEST").isPresent());
    }

    @Test
    @DisplayName("find By Token: should Return User When Found")
    void findByToken_ShouldReturnUser_WhenFound() {
        Users mockUser = new Users();
        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(Optional.of(mockUser));

        Users result = _userRepository.findByToken(TestConstants.FAKE_USER_TOKEN);
        assertEquals(mockUser, result);
    }

    @Test
    @DisplayName("find By Token: Should Throw Exception When Not Found")
    void findByToken_ShouldThrowException_WhenNotFound() {
        when(_jpaUserRepository.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                _userRepository.findByToken(TestConstants.FAKE_USER_TOKEN)
        );
    }

    @Test
    @DisplayName("is In Role: Should Return True When User Has This Role")
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
    @DisplayName("save: Should Call Jpa")
    void save_ShouldCallJpaSave() {
        Users user = new Users();
        _userRepository.save(user);
        verify(_jpaUserRepository, times(1)).saveAndFlush(user);
    }

    @Test
    @DisplayName("delete: Should Call Jpa")
    void delete_ShouldCallJpaDelete() {
        Users user = new Users();
        _userRepository.delete(user);
        verify(_jpaUserRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("find By Normalized Email: Should Call Jpa")
    void findByNormalizedEmail_ShouldCallJpa() {
        when(_jpaUserRepository.findByNormalizedEmail(anyString())).thenReturn(Optional.of(new Users()));
        assertTrue(_userRepository.findByNormalizedEmail(TestConstants.FAKE_EMAIL).isPresent());
    }

    @Test
    @DisplayName("is In Role: Should Throw Exception When User Token Is Invalid")
    void isInRole_ShouldThrowUserNotFound_WhenUserTokenIsInvalid() {
        when(_jpaUserRepository.findByToken("INVALID")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                _userRepository.isInRole(TestConstants.FAKE_ROLE, "INVALID"));
    }

    @Test
    @DisplayName("is In Role: Should Return False When User Does Not Have Role")
    void isInRole_ShouldReturnFalse_WhenUserDoesNotHaveTheRole() {
        Roles userRole = new Roles();
        userRole.setName("USER");
        Roles searchRole = new Roles();
        searchRole.setName("ADMIN");
        Users mockUser = new Users();
        mockUser.setRoles(Set.of(userRole));

        when(_jpaUserRepository.findByToken("T1")).thenReturn(Optional.of(mockUser));
        when(_jpaRoleRepository.findByName("ADMIN")).thenReturn(Optional.of(searchRole));

        assertFalse(_userRepository.isInRole("ADMIN", "T1"));
    }

    @Test
    @DisplayName("exist By Email: should return true when email exists")
    void existByEmail_ShouldReturnTrue_WhenEmailExists() {
        when(_jpaUserRepository.findByNormalizedEmail(anyString())).thenReturn(Optional.of(new Users()));
        assertTrue(_userRepository.existByEmail("TEST@TEST.PL"));
    }


    @Test
    @DisplayName("find All Users: Should Call Jpa with Specification and Pageable")
    @SuppressWarnings("unchecked")
    void findAllUsers_ShouldCallJpaWithSpecificationAndPageable() {
        Specification<Users> mockSpec = mock(Specification.class);
        Pageable mockPageable = mock(Pageable.class);
        Page<Users> mockPage = mock(Page.class);

        when(_jpaUserRepository.findAll(mockSpec, mockPageable)).thenReturn(mockPage);

        Page<Users> result = _userRepository.findAllUsers(mockSpec, mockPageable);

        assertEquals(mockPage, result);
        verify(_jpaUserRepository, times(1)).findAll(mockSpec, mockPageable);
    }
}