package com.example.restaurant.helpers;

import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class UserManagmentHelper {
  private final IUserRepository _userRepo;
  private final IRoleRepository _roleRepo;
  private final PasswordEncoder _passwordEncoder;

  public void deleteAccount(String userToken) {
    Users user = _userRepo.findByToken(userToken);

    user.setNormalizedEmail(SoftDeleteHelpers.markAsDelete(user.getNormalizedEmail()));
    user.setNormalizedUsername(SoftDeleteHelpers.markAsDelete(user.getNormalizedUsername()));

    user.setEmail(SoftDeleteHelpers.markAsDelete(user.getEmail()));
    user.setUsername(SoftDeleteHelpers.markAsDelete(user.getUsername()));
    user.setIsActive(false);
    user.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
    _userRepo.save(user);
  }

  public Users buildAndSaveUser(RegisterRequest request, String userRole, boolean isActive) {
    if (_userRepo.findByNormalizedEmail(request.getEmail().toUpperCase().trim()).isPresent())
      throw new EntityAlreadyExistsException("User with this email already exists");

    if (_userRepo.existsByUsername(request.getUsername().toUpperCase().trim()))
      throw new EntityAlreadyExistsException("User with this username already exists");

    Roles role = _roleRepo.setRole(userRole);

    Users user = new Users();
    user.setUsername(request.getUsername());
    user.setNormalizedUsername(request.getUsername().toUpperCase().trim());
    user.setEmail(request.getEmail());
    user.setNormalizedEmail(request.getEmail().toUpperCase().trim());
    user.setPassword(_passwordEncoder.encode(request.getPassword()));
    user.setIsActive(isActive);
    user.setRoles(Set.of(role));

    _userRepo.save(user);
    return user;
  }

  public void changeUsername(Users user, String userName) {
    String normalizedUserName = userName.toUpperCase().trim();

    if (normalizedUserName.equals(user.getNormalizedUsername()))
      throw new IllegalStateException("User name must be different");

    if (_userRepo.existsByUsername(normalizedUserName))
      throw new EntityAlreadyExistsException("User with this username already exists");

    user.setUsername(userName.trim());
    user.setNormalizedUsername(normalizedUserName);
  }
}
