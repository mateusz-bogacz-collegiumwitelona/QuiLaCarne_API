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
    public boolean changePassword(String token, String newPassword) {
        return _jpaUserRepository.findByToken(token).map(u -> {
            u.setPassword(_passwordEncoder.encode(newPassword));
            _jpaUserRepository.saveAndFlush(u);
            return true;
        }).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public boolean updatePassword(String userToken, String oldPassword, String newPassword) {
        return _jpaUserRepository.findByToken(userToken).map(u -> {
            if (!_passwordEncoder.matches(oldPassword, u.getPassword()))
                return false;

            u.setPassword(_passwordEncoder.encode(newPassword));
            _jpaUserRepository.saveAndFlush(u);

            return true;
        }).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public boolean updateEmail(String userToken, String email) {
        return _jpaUserRepository.findByToken(userToken).map(u -> {
            u.setPendingEmail(email);
            _jpaUserRepository.saveAndFlush(u);
            return true;
        }).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public boolean confirmEmailChange(String userToken) {
        return _jpaUserRepository.findByToken(userToken).map(u -> {
            if (u.getPendingEmail() == null)
                return false;

            u.setEmail(u.getPendingEmail());
            u.setNormalizedEmail(u.getPendingEmail().toUpperCase());
            u.setPendingEmail(null);

            _jpaUserRepository.saveAndFlush(u);
            return true;
        }).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public boolean activeUser(String userToken) {
        return _jpaUserRepository.findByToken(userToken).map(u -> {
            u.setIsActive(true);
            _jpaUserRepository.saveAndFlush(u);
            return true;
        }).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public boolean changeUserName(String userToken, String userName) {
        return _jpaUserRepository.findByToken(userToken).map(u -> {
            u.setUsername(userName);
            u.setNormalizedUsername(userName.toUpperCase());
            _jpaUserRepository.saveAndFlush(u);
            return true;
        }).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public boolean delete(String userToken) {
        return _jpaUserRepository.findByToken(userToken).map(u -> {
            String timestamp = String.valueOf(System.currentTimeMillis());

            u.setNormalizedEmail("DELETED_" + timestamp + "_" + u.getNormalizedEmail());
            u.setNormalizedUsername("DELETED_" + timestamp + "_" + u.getNormalizedUsername());

            u.setEmail("deleted_" + timestamp + "_" + u.getEmail());
            u.setUsername("deleted_" + timestamp + "_" + u.getUsername());

            _jpaUserRepository.saveAndFlush(u);
            _jpaUserRepository.delete(u);

            return true;
        }).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    public boolean isInRole(String roleName, String userToken) {
        Users user = _jpaUserRepository.findByToken(userToken).orElseThrow(() -> new UserNotFoundException("User not found"));

        Roles role = _jpaRoleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found"));

        return user.getRoles().contains(role);
    }
}
