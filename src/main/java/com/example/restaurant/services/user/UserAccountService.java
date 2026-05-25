package com.example.restaurant.services.user;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.helpers.UserManagmentHelper;
import com.example.restaurant.helpers.staics.RoleType;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import jakarta.transaction.Transactional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountService {
  private final IUserRepository _userRepo;
  private final IRoleRepository _roleRepository;
  private final PasswordEncoder _passwordEncoder;
  private final UserManagmentHelper _userHelper;
  private final IVerificationTokenServices _tokenServices;

  @Transactional
  @CacheEvict(value = "usersList", allEntries = true)
  public void activeUser(String userToken) {
    Users user = _userRepo.findByToken(userToken);

    if (Boolean.TRUE.equals(user.getIsActive()))
      throw new IllegalStateException("User is already active");

    user.setIsActive(true);
    _userRepo.save(user);
    log.info("Active user {} by {}", userToken, user.getUsername());
  }

  @Auditable(action = "DELETE_ACCOUNT")
  @CacheEvict(value = "usersList", allEntries = true)
  public void delete(String userToken) {
    _userHelper.deleteAccount(userToken);
    _tokenServices.revokeTokensForUser(userToken, TokenTypeEnum.REFRESH_TOKEN);
    log.info("User {} has been deleted", userToken);
  }

  @Transactional
  @Auditable(action = "CREATE_OAUTH_USER")
  @CacheEvict(value = "usersList", allEntries = true)
  public String createOAuthUser(String email) {
    String baseUserName = email.split("@")[0];
    String originalBase = baseUserName;
    int counter = 1;

    while (_userRepo.existsByUsername(baseUserName.toUpperCase().trim())) {
      baseUserName = originalBase + counter;
      counter++;
    }

    String randomPassword = UUID.randomUUID() + "G00G1E#";

    Roles role = _roleRepository.setRole(RoleType.ROLE_CLIENT);

    Users user = new Users();
    user.setUsername(baseUserName);
    user.setNormalizedUsername(baseUserName.trim().toUpperCase());
    user.setEmail(email);
    user.setNormalizedEmail(email.toUpperCase().trim());
    user.setPassword(_passwordEncoder.encode(randomPassword));
    user.setIsActive(true);
    user.setRoles(Set.of(role));

    _userRepo.save(user);

    log.info("Created user {} by {}", baseUserName, user.getUsername());

    return baseUserName;
  }

  @Transactional
  @Auditable(action = "UPDATE_USERNAME")
  @CacheEvict(value = "usersList", allEntries = true)
  public void updateUserName(String userName, String userToken) {
    Users user = _userRepo.findByToken(userToken);
    _userHelper.changeUsername(user, userName);
    _userRepo.save(user);
    _tokenServices.revokeTokensForUser(userToken, TokenTypeEnum.REFRESH_TOKEN);
    log.info("Updated user {} by {}", userName, user.getUsername());
  }
}
