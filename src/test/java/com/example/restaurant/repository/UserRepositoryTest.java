package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        request.setUsername(TestConstants.FAKE_USERNAME);
        request.setPassword(TestConstants.FAKE_PASSWORD);

        Roles mockRole = new Roles();
        mockRole.setName("ROLE_CLIENT");

        Users mockUser = new Users();
        mockUser.setToken(TestConstants.FAKE_TOKEN);

        when(_roleRepository.setRole("ROLE_CLIENT"))
                .thenReturn(mockRole
                );

        when(_passwordEncoder
                .encode(TestConstants.FAKE_PASSWORD)
        ).thenReturn(TestConstants.FAKE_HASH);

        when(_jpaUserRepository
                .saveAndFlush(any(Users.class))
        ).thenReturn(mockUser);

        String token = _userRepository.createUser(request, "ROLE_CLIENT", false);

        assertEquals(TestConstants.FAKE_TOKEN, token);
        verify(_passwordEncoder).encode(TestConstants.FAKE_PASSWORD);
        verify(_jpaUserRepository).saveAndFlush(argThat(user ->
                user.getPassword().equals(TestConstants.FAKE_HASH) &&
                        user.getUsername().equals(TestConstants.FAKE_USERNAME)
        ));
    }

    @Test
    void findMinimalByEmail_ShouldReturnDTO_WhenUserIsExist() {
        Users user = new Users();
        user.setToken(TestConstants.FAKE_TOKEN);
        user.setUsername(TestConstants.FAKE_USERNAME);
        user.setEmail(TestConstants.FAKE_EMAIL);

        when(_jpaUserRepository
                .findByEmail(TestConstants.FAKE_EMAIL)
        ).thenReturn(Optional.of(user));

        var result = _userRepository
                .findMinimalByEmail(TestConstants.FAKE_EMAIL);

        assertTrue(result.isPresent());

        assertEquals(
                TestConstants.FAKE_TOKEN,
                result.get().token()
        );

        assertEquals(
                TestConstants.FAKE_USERNAME,
                result.get().username()
        );
    }

    @Test
    void changePassword_ShouldReturnHashAndSaveUser() {
        Users user = new Users();
        user.setToken(TestConstants.FAKE_TOKEN);

        when(_jpaUserRepository
                .findByToken(TestConstants.FAKE_TOKEN)
        ).thenReturn(Optional.of(user));

        when(_passwordEncoder
                .encode(TestConstants.FAKE_PASSWORD)
        ).thenReturn(TestConstants.FAKE_HASH);

        boolean result = _userRepository
                .changePassword(
                        TestConstants.FAKE_TOKEN,
                        TestConstants.FAKE_PASSWORD
                );

        assertTrue(result);

        assertEquals(
                TestConstants.FAKE_HASH,
                user.getPassword()
        );

        verify(_jpaUserRepository).saveAndFlush(user);
    }
}
