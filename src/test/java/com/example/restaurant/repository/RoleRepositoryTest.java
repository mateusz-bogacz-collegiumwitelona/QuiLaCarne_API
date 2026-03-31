package com.example.restaurant.repository;

import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleRepositoryTest {
    @Mock
    private IJpaRoleRepository _jpaRoleRepository;

    @InjectMocks
    private RoleRepository _roleRepository;

    @Test
    @DisplayName("Set role: should throw exception if role not found")
    void setRole_ShouldThrowException_WhenRoleNotFound() {
        when(_jpaRoleRepository.findByName("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            _roleRepository.setRole("NON_EXISTENT");
        });
    }

    @Test
    @DisplayName("Set role: should return role if found")
    void setRole_ShouldReturnRole_WhenFound() {
        Roles mockRole = new Roles();
        mockRole.setName("USER");
        when(_jpaRoleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));

        Roles result = _roleRepository.setRole("USER");

        assertEquals("USER", result.getName());
        verify(_jpaRoleRepository).findByName("USER");
    }

    @Test
    @DisplayName("Is role exist: should return true if exist")
    void isRoleExists_ShouldReturnTrue_WhenExists() {
        when(_jpaRoleRepository.findByName("ADMIN")).thenReturn(Optional.of(new Roles()));
        assertTrue(_roleRepository.isRoleExists("ADMIN"));
    }

}
