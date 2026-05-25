package com.example.restaurant.services.user;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.ChangePasswordRequest;
import com.example.restaurant.dto.request.Verify2faRequest;
import com.example.restaurant.dto.response.Generate2faResponse;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.models.Users;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.TwoFactorServices;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSecurityService {
  private final IUserRepository _userRepo;
  private final PasswordEncoder _passwordEncoder;
  private final TwoFactorServices _2faServices;
  private final IVerificationTokenServices _tokenServices;

  @Auditable(action = "UPDATE_PASSWORD")
  @Transactional
  public void updatePassword(String userToken, ChangePasswordRequest request) {
    if (!request.getConfirmPassword().equals(request.getPassword()))
      throw new IllegalStateException("Passwords do not match");

    Users user = _userRepo.findByToken(userToken);

    if (!_passwordEncoder.matches(request.getOldPassword(), user.getPassword()))
      throw new BadCredentialsException("Invalid old password");

    user.setPassword(_passwordEncoder.encode(request.getConfirmPassword()));
    _userRepo.save(user);
    _tokenServices.revokeTokensForUser(userToken, TokenTypeEnum.REFRESH_TOKEN);
    log.info("Updated user password {}", user.getUsername());
  }

  @Transactional
  public void changePassword(String token, String newPassword) {
    Users user = _userRepo.findByToken(token);
    user.setPassword(_passwordEncoder.encode(newPassword));
    _userRepo.save(user);
    _tokenServices.revokeTokensForUser(token, TokenTypeEnum.REFRESH_TOKEN);
    log.info("Updated user password {}", user.getUsername());
  }

  @Transactional
  @Auditable(action = "GENERATE_2FA_SECRET")
  public Generate2faResponse generate2fa(String userToken) {
    Users user = _userRepo.findByToken(userToken);

    if (user.getIsTwoFactorEnabled() != null && user.getIsTwoFactorEnabled())
      throw new IllegalStateException("2FA is already enabled for this user");

    String secret = _2faServices.generateNewSecret();

    user.setMfaSecret(secret);
    _userRepo.save(user);

    String qrUriCode = _2faServices.generateQrCodeImageUri(secret, user.getUsername());
    log.info("Generated data for QR code for user {}", user.getUsername());
    return Generate2faResponse.builder().qrCodeUri(qrUriCode).manualCode(secret).build();
  }

  @Transactional
  @Auditable(action = "ENABLE_2FA")
  public void verifyAndEnable2fa(String userToken, Verify2faRequest request) {
    Users user = _userRepo.findByToken(userToken);

    if (user.getIsTwoFactorEnabled() != null && user.getIsTwoFactorEnabled())
      throw new IllegalStateException("2FA is already enabled");

    if (user.getMfaSecret() == null)
      throw new IllegalStateException(
          "2FA secret is not generated yet. Please call generate first.");

    boolean isValid = _2faServices.isOptValid(user.getMfaSecret(), request.getCode());

    if (!isValid) throw new IllegalStateException("Invalid 2FA code. Try again.");

    user.setIsTwoFactorEnabled(true);

    _userRepo.save(user);
    _tokenServices.revokeTokensForUser(userToken, TokenTypeEnum.REFRESH_TOKEN);
    log.info("Verifying 2FA code for user {}", user.getUsername());
  }
}
