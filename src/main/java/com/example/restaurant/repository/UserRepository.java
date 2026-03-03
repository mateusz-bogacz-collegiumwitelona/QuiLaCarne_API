package com.example.restaurant.repository;

import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {
    private final IRoleRepository _roleRepository;
    private final PasswordEncoder _passwordEncoder;
    private final IJpaUserRepository _jpaUserRepository;

    @Transactional
    public String createUser(RegisterRequest request, String userRole, boolean isActive)
    {
        Roles role = _roleRepository.setRole(userRole);

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(_passwordEncoder.encode(request.getPassword()));
        user.setIsActive(isActive);
        user.setRoles(Set.of(role));

        Users saved = _jpaUserRepository.save(user);
        return saved.getToken();
    }

    public boolean existsByUsername(String username) {
        return _jpaUserRepository.findByUsername(username).isPresent();
    }
}
