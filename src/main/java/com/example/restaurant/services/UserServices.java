package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import com.example.restaurant.services.interfaces.IUserServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServices implements IUserServices {
    private final IUserRepository _userRepo;
    private final EmailServices _emailServices;
    private final IVerificationTokenRepository _tokenRepo;
    private final IRoleRepository _roleRepository;
    private final PasswordEncoder _passwordEncoder;

    @Override
    @Transactional
    public String create(RegisterRequest request, String userRole, boolean isActive) {
        Roles role = _roleRepository.setRole(userRole);

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setNormalizedUsername(request.getUsername().toUpperCase().trim());
        user.setEmail(request.getEmail());
        user.setNormalizedEmail(request.getEmail().toUpperCase().trim());
        user.setPassword(_passwordEncoder.encode(request.getPassword()));
        user.setIsActive(isActive);
        user.setRoles(Set.of(role));

        _userRepo.save(user);
        return user.getToken();
    }

    @Override
    @Transactional
    public void activeUser(String userToken) {
        Users user = _userRepo.findByToken(userToken);

        user.setIsActive(true);
        _userRepo.save(user);
    }


    @Override
    public Optional<UserDomain> findMinimalByEmail(String email) {
        return _userRepo.findByNormalizedEmail(email.toUpperCase().trim())
                .map(u -> new UserDomain(
                        u.getToken(), u.getUsername(),
                        u.getNormalizedUsername(), u.getEmail(), u.getNormalizedEmail()
                ));
    }

    @Override
    @Auditable(action = "UPDATE_PASSWORD")
    @Transactional
    public ResultHandler<Void> updatePassword(String userToken, UpdatePasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword()))
            return ResultHandler.failure(
                    "Passwords do not match",
                    HttpStatus.BAD_REQUEST.value()
            );

        Users user = _userRepo.findByToken(userToken);

        if (!_passwordEncoder.matches(request.getOldPassword(), user.getPassword()))
            throw new BadCredentialsException("Invalid old password");

        user.setPassword(_passwordEncoder.encode(request.getConfirmPassword()));

        _userRepo.save(user);

        return ResultHandler.success(
                "Password updated",
                HttpStatus.OK.value()
        );
    }

    @Override
    @Transactional
    public void changePassword(String token, String newPassword) {
        Users user = _userRepo.findByToken(token);
        user.setPassword(_passwordEncoder.encode(newPassword));
        _userRepo.save(user);
    }

    @Override
    @Auditable(action = "UPDATE_EMAIL")
    @Transactional
    public ResultHandler<Void> updateEmail(String userToken, String email) {
        var isUserExist = findMinimalByEmail(email);

        if (isUserExist.isPresent() && !isUserExist.get().token().equals(userToken))
            return ResultHandler.failure(
                    "The email is being used by someone else",
                    HttpStatus.BAD_REQUEST.value()
            );

        Users user = _userRepo.findByToken(userToken);

        user.setPendingEmail(email);

        _userRepo.save(user);

        String token = _tokenRepo.createToken(
                userToken,
                TokenTypeEnum.EMAIL_UPDATE,
                60
        );

        _emailServices.sendEmailChangeVerification(email, token);

        return ResultHandler.success(
                "Verification link sent to the new email",
                HttpStatus.OK.value()
        );
    }

    @Override
    @Transactional
    @Auditable(action = "CONFIRM_EMAIL_CHANGE")
    public ResultHandler<Void> confirmEmailChange(String userToken, String token) {
        boolean isValidToken = _tokenRepo.validateToken(userToken, token, TokenTypeEnum.EMAIL_UPDATE);

        if (!isValidToken)
            return ResultHandler.failure(
                    "Invalid or expired token",
                    HttpStatus.BAD_REQUEST.value()
            );

        Users user = _userRepo.findByToken(userToken);

        if (user.getPendingEmail() == null)
            throw new IllegalStateException("No pending email to confirm");

        user.setEmail(user.getPendingEmail());
        user.setNormalizedEmail(user.getPendingEmail().toUpperCase());
        user.setPendingEmail(null);

        _userRepo.save(user);

        return ResultHandler.success(
                "Email updated successfully",
                HttpStatus.OK.value()
        );
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_USERNAME")
    public ResultHandler<Void> updateUserName(String userName, String userToken) {
        if (_userRepo.existsByUsername(userName.toUpperCase().trim()))
            return ResultHandler.failure(
                    "Username is already taken",
                    HttpStatus.BAD_REQUEST.value()
            );

        Users user = _userRepo.findByToken(userToken);
        user.setUsername(userName);
        user.setNormalizedUsername(userName.toUpperCase());

        _userRepo.save(user);

        return ResultHandler.success(
                "User name changed successfully",
                HttpStatus.OK.value()
        );
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_ACCOUNT")
    public ResultHandler<Void> deleteAccount(String userToken) {
        Users user = _userRepo.findByToken(userToken);

        String timestamp = String.valueOf(System.currentTimeMillis());

        user.setNormalizedEmail("DELETED_" + timestamp + "_" + user.getNormalizedEmail());
        user.setNormalizedUsername("DELETED_" + timestamp + "_" + user.getNormalizedUsername());

        user.setEmail("deleted_" + timestamp + "_" + user.getEmail());
        user.setUsername("deleted_" + timestamp + "_" + user.getUsername());
        user.setIsActive(false);
        _userRepo.save(user);

        return ResultHandler.success(
                "User deleted successfully",
                HttpStatus.OK.value()
        );
    }
}
