package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.AddEmployeeRequest;
import com.example.restaurant.dto.request.EditEmployeeRequest;
import com.example.restaurant.dto.request.RegisterRequest;
import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.InvalidDateException;
import com.example.restaurant.helpers.SoftDeleteHelpers;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IUserServices;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServices implements IUserServices {
    private final IUserRepository _userRepo;
    private final EmailServices _emailServices;
    private final IRoleRepository _roleRepository;
    private final PasswordEncoder _passwordEncoder;
    private final IVerificationTokenServices _tokenServices;

    @Override
    @Transactional
    public String create(RegisterRequest request, String userRole, boolean isActive) {
        return buildAndSaveUser(request, userRole, isActive);
    }

    @Override
    @Transactional
    public void activeUser(String userToken) {
        Users user = _userRepo.findByToken(userToken);

        if (user.getIsActive())
            throw new IllegalStateException("User is already active");

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
    public void updatePassword(String userToken, UpdatePasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword()))
            throw new IllegalStateException("Passwords do not match");

        Users user = _userRepo.findByToken(userToken);

        if (!_passwordEncoder.matches(request.getOldPassword(), user.getPassword()))
            throw new BadCredentialsException("Invalid old password");

        user.setPassword(_passwordEncoder.encode(request.getConfirmPassword()));

        _userRepo.save(user);

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
    public void updateEmail(String userToken, String email) {
        String normalizedEmail = email.toUpperCase().trim();

        var existingUser = findMinimalByEmail(email);
        if (existingUser.isPresent() && !existingUser.get().token().equals(userToken))
            throw new EntityAlreadyExistsException("The email is being used by someone else");

        Users user = _userRepo.findByToken(userToken);

        if (normalizedEmail.equals(user.getNormalizedEmail()))
            throw new IllegalStateException("You are already using this email address");


        user.setPendingEmail(email);
        _userRepo.save(user);

        String token = _tokenServices.createToken(userToken, TokenTypeEnum.EMAIL_UPDATE, 60);
        _emailServices.sendEmailChangeVerification(email, token);
    }

    @Override
    @Transactional
    @Auditable(action = "CONFIRM_EMAIL_CHANGE")
    public void confirmEmailChange(String userToken, String token) {
        boolean isValidToken = _tokenServices.validateToken(userToken, token, TokenTypeEnum.EMAIL_UPDATE);

        if (!isValidToken)
            throw new InvalidDateException("Invalid or expired token");

        Users user = _userRepo.findByToken(userToken);

        if (user.getPendingEmail() == null)
            throw new IllegalStateException("No pending email to confirm");

        user.setEmail(user.getPendingEmail());
        user.setNormalizedEmail(user.getPendingEmail().toUpperCase().trim());
        user.setPendingEmail(null);

        _userRepo.save(user);
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_USERNAME")
    public void updateUserName(String userName, String userToken) {
        String normalizedNewName = userName.toUpperCase().trim();

        Users user = _userRepo.findByToken(userToken);

        if (normalizedNewName.equals(user.getNormalizedUsername()))
            throw new IllegalStateException("New username must be different from the current one");

        if (_userRepo.existsByUsername(normalizedNewName))
            throw new EntityAlreadyExistsException("Username is already taken");

        user.setUsername(userName);
        user.setNormalizedUsername(normalizedNewName);
        _userRepo.save(user);
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_ACCOUNT")
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

    @Override
    @Transactional
    @Auditable(action = "ADD_NEW_EMPLOYEE")
    public void createEmployee(AddEmployeeRequest request) {
        if (request.isAdmin()) {
            buildAndSaveUser(request.getRegister(), "ROLE_MANAGER", true);
        } else {
            buildAndSaveUser(request.getRegister(), "ROLE_WAITER", true);
        }
    }

    @Override
    @Transactional
    @Auditable(action = "EDIT_EMPLOYEE")
    public void editEmployee(EditEmployeeRequest request) {
        Users employee = _userRepo.findByToken(request.getEmployeeToken());

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String normalizedEmail = request.getEmail().toUpperCase().trim();

            if (employee.getNormalizedEmail().equals(normalizedEmail))
                throw new IllegalStateException("Email must be different");

            if (_userRepo.findByNormalizedEmail(normalizedEmail).isPresent())
                throw new EntityAlreadyExistsException("User with this email already exists");

            employee.setEmail(request.getEmail().trim());
            employee.setNormalizedEmail(normalizedEmail);
        }

        if (request.getUserName() != null && !request.getUserName().isBlank()) {
            String normalizedUserName = request.getUserName().toUpperCase().trim();

            if (employee.getNormalizedUsername().equals(normalizedUserName))
                throw new IllegalStateException("User name must be different");

            if (_userRepo.existsByUsername(normalizedUserName))
                throw new EntityAlreadyExistsException("User with this username already exists");

            employee.setUsername(request.getUserName().trim());
            employee.setNormalizedUsername(normalizedUserName);
        }

        _userRepo.save(employee);
    }

    private String buildAndSaveUser(RegisterRequest request, String userRole, boolean isActive) {
        if (_userRepo.findByNormalizedEmail(request.getEmail().toUpperCase().trim()).isPresent())
            throw new EntityAlreadyExistsException("User with this email already exists");

        if (_userRepo.existsByUsername(request.getUsername().toUpperCase().trim()))
            throw new EntityAlreadyExistsException("User with this username already exists");

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
}
