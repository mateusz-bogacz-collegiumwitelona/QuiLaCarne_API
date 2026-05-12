package com.example.restaurant.services.user;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.InvalidDateException;
import com.example.restaurant.helpers.UserManagmentHelper;
import com.example.restaurant.models.Users;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.EmailServices;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserIdentityService {
  private final IUserRepository _userRepo;
  private final IVerificationTokenServices _tokenServices;
  private final EmailServices _emailServices;
  private final UserManagmentHelper _userHelper;

  @Auditable(action = "UPDATE_EMAIL")
  @Transactional
  public void updateEmail(String userToken, String email) {
    String normalizedEmail = email.toUpperCase().trim();

    var existingUser = findMinimalByEmail(email);
    if (existingUser.isPresent() && !userToken.equals(existingUser.get().token()))
      throw new EntityAlreadyExistsException("The email is being used by someone else");

    Users user = _userRepo.findByToken(userToken);

    if (user.getNormalizedEmail().equals(normalizedEmail))
      throw new IllegalStateException("You are already using this email address");

    user.setPendingEmail(email);
    _userRepo.save(user);

    String token = _tokenServices.createToken(userToken, TokenTypeEnum.EMAIL_UPDATE, 60);
    _emailServices.sendEmailChangeVerification(email, token);
  }

  @Transactional
  @Auditable(action = "CONFIRM_EMAIL_CHANGE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void confirmEmailChange(String userToken, String token) {
    boolean isValidToken =
        _tokenServices.validateToken(userToken, token, TokenTypeEnum.EMAIL_UPDATE);

    if (!isValidToken) throw new InvalidDateException("Invalid or expired token");

    Users user = _userRepo.findByToken(userToken);

    if (user.getPendingEmail() == null)
      throw new IllegalStateException("No pending email to confirm");

    user.setEmail(user.getPendingEmail());
    user.setNormalizedEmail(user.getPendingEmail().toUpperCase().trim());
    user.setPendingEmail(null);

    _userRepo.save(user);
  }

  public Optional<UserDomain> findMinimalByEmail(String email) {
    return _userRepo
        .findByNormalizedEmail(email.toUpperCase().trim())
        .map(
            u ->
                new UserDomain(
                    u.getToken(),
                    u.getUsername(),
                    u.getNormalizedUsername(),
                    u.getEmail(),
                    u.getNormalizedEmail()));
  }

  @Transactional
  @CacheEvict(value = "usersList", allEntries = true)
  public String create(RegisterRequest request, String userRole, boolean isActive) {
    return _userHelper.buildAndSaveUser(request, userRole, isActive).getToken();
  }
}
