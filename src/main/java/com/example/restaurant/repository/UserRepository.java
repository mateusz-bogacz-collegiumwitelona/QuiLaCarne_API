package com.example.restaurant.repository;

import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.exceptions.UserNotFoundException;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {
    private final IRoleRepository _roleRepository;
    private final PasswordEncoder _passwordEncoder;
    private final IJpaUserRepository _jpaUserRepository;
    private final IJpaRoleRepository _jpaRoleRepository;

    @Override
    @Transactional
    public String createUser(RegisterRequest request, String userRole, boolean isActive) {
        Roles role = _roleRepository.setRole(userRole);

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setNormalizedUsername(request.getUsername().toUpperCase().trim());
        user.setEmail(request.getEmail());
        user.setNormalizedEmail(request.getEmail().toUpperCase().trim());
        user.setPassword(_passwordEncoder.encode(request.getPassword()));
        user.setIsActive(isActive);
        user.setRoles(Set.of(role));

        Users saved = _jpaUserRepository.saveAndFlush(user);
        return saved.getToken();
    }

    @Override
    public boolean existsByUsername(String username) {
        return _jpaUserRepository
                .findByNormalizedUsername(
                        username.toUpperCase().trim()
                ).isPresent();
    }

    @Override
    public Optional<UserDomain> findMinimalByEmail(String email) {
        return _jpaUserRepository.findByNormalizedEmail(email.toUpperCase().trim())
                .map(u -> new UserDomain(
                                u.getToken(),
                                u.getUsername(),
                                u.getNormalizedUsername(),
                                u.getEmail(),
                                u.getNormalizedEmail()
                        )
                );
    }

    @Override
    @Transactional
    public void changePassword(String token, String newPassword) {
        Users user = _jpaUserRepository.findByToken(token).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        user.setPassword(_passwordEncoder.encode(newPassword));
        _jpaUserRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public void updatePassword(String userToken, String oldPassword, String newPassword) {
        Users user = _jpaUserRepository.findByToken(userToken).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (!_passwordEncoder.matches(oldPassword, user.getPassword()))
            throw new BadCredentialsException("Invalid old password");

        user.setPassword(_passwordEncoder.encode(newPassword));
        _jpaUserRepository.saveAndFlush(user);
    }

    @Override
    public Users findByToken(String token) {
        return _jpaUserRepository.findByToken(token).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );
    }


    @Override
    @Transactional
    public void updateEmail(String userToken, String email) {
        Users user = _jpaUserRepository.findByToken(userToken).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );
        user.setPendingEmail(email);
        _jpaUserRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public void confirmEmailChange(String userToken) {
        Users user = _jpaUserRepository.findByToken(userToken).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (user.getPendingEmail() == null)
            throw new IllegalStateException("No pending email to confirm");

        user.setEmail(user.getPendingEmail());
        user.setNormalizedEmail(user.getPendingEmail().toUpperCase());
        user.setPendingEmail(null);

        _jpaUserRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public void activeUser(String userToken) {
        Users user = _jpaUserRepository.findByToken(userToken).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        user.setIsActive(true);
        _jpaUserRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public void changeUserName(String userToken, String userName) {
        Users user = _jpaUserRepository.findByToken(userToken).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );
        user.setUsername(userName);
        user.setNormalizedUsername(userName.toUpperCase());
        _jpaUserRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public void delete(String userToken) {
        Users user = _jpaUserRepository.findByToken(userToken).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        String timestamp = String.valueOf(System.currentTimeMillis());

        user.setNormalizedEmail("DELETED_" + timestamp + "_" + user.getNormalizedEmail());
        user.setNormalizedUsername("DELETED_" + timestamp + "_" + user.getNormalizedUsername());

        user.setEmail("deleted_" + timestamp + "_" + user.getEmail());
        user.setUsername("deleted_" + timestamp + "_" + user.getUsername());

        _jpaUserRepository.saveAndFlush(user);
        _jpaUserRepository.delete(user);
    }

    @Override
    public boolean isInRole(String roleName, String userToken) {
        Users user = _jpaUserRepository.findByToken(userToken).orElseThrow(() -> new UserNotFoundException("User not found"));

        Roles role = _jpaRoleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found"));

        return user.getRoles().contains(role);
    }

    @Override
    public Optional<Users> findByNormalizedUsername(String username) {
        return _jpaUserRepository.findByNormalizedUsername(username);
    }
}
