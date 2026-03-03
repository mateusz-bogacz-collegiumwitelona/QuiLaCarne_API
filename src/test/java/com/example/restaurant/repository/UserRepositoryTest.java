package com.example.restaurant.repository;

import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void createUser_ShouldHashPasswordAndRetrunToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("fake-username");
        request.setPassword("fake-password");

        Roles mockRole = new Roles();
        mockRole.setName("ROLE_CLIENT");

        Users mockUser = new Users();
        mockUser.setToken("fake-token");

        when(_roleRepository.setRole("ROLE_CLIENT")).thenReturn(mockRole);
        when(_passwordEncoder.encode("fake-password")).thenReturn("hashedPassword");
        when(_jpaUserRepository.saveAndFlush(any(Users.class))).thenReturn(mockUser);

        String token = _userRepository.createUser(request, "ROLE_CLIENT", false);

        assertEquals("fake-token", token);
        verify(_passwordEncoder).encode("fake-password");
        verify(_jpaUserRepository).saveAndFlush(argThat(user ->
                user.getPassword().equals("hashedPassword") &&
                        user.getUsername().equals("fake-username")
        ));
    }
}
