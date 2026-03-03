package com.example.restaurant.repository;

import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleRepositoryTest {
    @Mock
    private IJpaRoleRepository _jpaRoleRepository;

    @InjectMocks
    private RoleRepository _roleRepository;

    @Test
    void setRole_ShouldThrowException_WhenRoleNotFound()
    {
        when(_jpaRoleRepository.findByName("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
           _roleRepository.setRole("NON_EXISTENT");
        });
    }

}
