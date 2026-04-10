package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.Generate2faResponse;
import com.example.restaurant.dto.response.UserListResponse;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.InvalidDateException;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.SoftDeleteHelpers;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.base.BaseNamedEntity;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IUserServices;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import jakarta.persistence.criteria.JoinType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServices implements IUserServices {
    private final IUserRepository _userRepo;
    private final EmailServices _emailServices;
    private final IRoleRepository _roleRepository;
    private final PasswordEncoder _passwordEncoder;
    private final IVerificationTokenServices _tokenServices;
    private final TwoFactorServices _2faServices;
    private final NotificationServices _notification;

    private final String ROLE_MANAGER = "ROLE_MANAGER";
    private final String ROLE_WAITER = "ROLE_WAITER";

    private static final String EMPLOYEE_ENTITY_TYPE = "EMPLOYEE";

    @Override
    @Transactional
    @CacheEvict(value = "usersList", allEntries = true)
    public String create(RegisterRequest request, String userRole, boolean isActive) {
        return buildAndSaveUser(request, userRole, isActive).getToken();
    }

    @Override
    @Transactional
    @CacheEvict(value = "usersList", allEntries = true)
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
    public void updatePassword(String userToken, ChangePasswordRequest request) {
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
    @CacheEvict(value = "usersList", allEntries = true)
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
    @CacheEvict(value = "usersList", allEntries = true)
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
    @CacheEvict(value = "usersList", allEntries = true)
    public void delete(String userToken) {
        deleteAccount(userToken);
    }

    @Override
    @Transactional
    @Auditable(action = "ADD_NEW_EMPLOYEE")
    @CacheEvict(value = "usersList", allEntries = true)
    public void createEmployee(AddEmployeeRequest request) {
        Users employee;
        if (request.isAdmin()) {
            employee = buildAndSaveUser(request.getRegister(), ROLE_MANAGER, true);
        } else {
            employee = buildAndSaveUser(request.getRegister(), ROLE_WAITER, true);
        }

        WebSocketEvent<UserListResponse> event = WebSocketEvent.created(
                EMPLOYEE_ENTITY_TYPE,
                employee.getToken(),
                createPayload(employee)
        );
        _notification.sendEventToTopic("/personnel/updates", event);
    }

    @Override
    @Transactional
    @Auditable(action = "EDIT_EMPLOYEE")
    @CacheEvict(value = "usersList", allEntries = true)
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

        WebSocketEvent<UserListResponse> event = WebSocketEvent.updated(
                EMPLOYEE_ENTITY_TYPE,
                employee.getToken(),
                createPayload(employee)
        );
        _notification.sendEventToTopic("/personnel/updates", event);
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_EMPLOYEE_PASSWORD")
    public void changeEmployeePassword(String adminToken, ChangeEmployeePasswordRequest request) {
        if (adminToken.trim().equals(request.getEmployeeToken().trim()))
            throw new IllegalStateException("You can't change your own password");

        if (!request.getPassword().equals(request.getConfirmPassword()))
            throw new IllegalStateException("Passwords do not match");

        Users user = _userRepo.findByToken(request.getEmployeeToken());

        user.setPassword(_passwordEncoder.encode(request.getConfirmPassword()));

        _userRepo.save(user);
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_EMPLOYEE_ROLE")
    @CacheEvict(value = "usersList", allEntries = true)
    public void changeEmployeeRole(String adminToken, ChangeEmployeeRoleRequest request) {
        if (adminToken.trim().equals(request.getEmployeeToken().trim()))
            throw new IllegalStateException("You can't change your own role");

        Users user = _userRepo.findByToken(request.getEmployeeToken());

        boolean isCurrentlyManager = user.getRoles().stream().anyMatch(r -> r.getName().equals(ROLE_MANAGER));

        if (request.isAdmin() && isCurrentlyManager)
            throw new IllegalStateException("User is already a Manager");

        if (!request.isAdmin() && !isCurrentlyManager)
            throw new IllegalStateException("User is already a Waiter");

        Roles role;
        if (request.isAdmin()) {
            role = _roleRepository.setRole(ROLE_MANAGER);
        } else {
            role = _roleRepository.setRole(ROLE_WAITER);
        }
        user.setRoles(Set.of(role));

        _userRepo.save(user);

        WebSocketEvent<UserListResponse> event = WebSocketEvent.updated(
                EMPLOYEE_ENTITY_TYPE,
                user.getToken(),
                createPayload(user)
        );
        _notification.sendEventToTopic("/personnel/updates", event);
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_EMPLOYEE_AVALAIBLE")
    @CacheEvict(value = "usersList", allEntries = true)
    public void blockEmployee(String adminToken, BlockEmployeeRequest request) {
        if (adminToken.trim().equals(request.getEmployeeToken().trim()))
            throw new IllegalStateException("You can't change your own availability");

        Users user = _userRepo.findByToken(request.getEmployeeToken());

        if (request.isAvailable() == user.getIsActive())
            throw new IllegalStateException("Employee availability is already set to this state");

        user.setIsActive(request.isAvailable());

        _userRepo.save(user);

        WebSocketEvent<UserListResponse> event = WebSocketEvent.updated(
                EMPLOYEE_ENTITY_TYPE,
                user.getToken(),
                createPayload(user)
        );
        _notification.sendEventToTopic("/personnel/updates", event);
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_EMPLOYEE")
    @CacheEvict(value = "usersList", allEntries = true)
    public void deleteEmployee(String adminToken, String employeeToken) {
        if (adminToken.trim().equals(employeeToken.trim()))
            throw new IllegalStateException("You can't delete yourself");

        delete(employeeToken);

        WebSocketEvent<Void> event = WebSocketEvent.deleted(EMPLOYEE_ENTITY_TYPE, employeeToken);
        _notification.sendEventToTopic("/personnel/updates", event);
    }

    @Override
    @Cacheable(value = "usersList", key = "#filter.toString() + '-' + #pagged.toString()")
    public PagedResult<UserListResponse> getUserList(UserFilterRequest filter, PaggedRequest pagged) {
        String directionStr = (filter.getSortDirection() != null && !filter.getSortDirection().isBlank())
                ? filter.getSortDirection() : "ASC";
        Sort.Direction direction = Sort.Direction.fromString(directionStr);

        String sortByField = filter.getSortBy();
        if (sortByField == null || sortByField.isBlank() ||
                sortByField.equalsIgnoreCase("ASC") || sortByField.equalsIgnoreCase("DESC")) {
            sortByField = "createdAt";
        }

        Sort sort = Sort.by(direction, sortByField);

        PageRequest pageRequest = PageRequest.of(pagged.getPage() - 1, pagged.getSize(), sort);

        Specification<Users> spec = (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String searchPattern = "%" + filter.getSearch().toUpperCase().trim() + "%";
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.upper(root.get("username")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.upper(root.get("email")), searchPattern)
                ));
            }

            if (filter.getIsActive() != null)
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(
                        root.get("isActive"),
                        filter.getIsActive()
                ));

            if (filter.getRole() != null && !filter.getRole().isBlank()) {
                var joinRole = root.join("roles", JoinType.INNER);
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(
                        joinRole.get("name"),
                        filter.getRole().trim()
                ));
            }

            return predicates;
        };

        Page<Users> usersPage = _userRepo.findAllUsers(spec, pageRequest);

        Page<UserListResponse> response = usersPage.map(u -> UserListResponse
                .builder()
                .token(u.getToken())
                .username(u.getUsername())
                .email(u.getEmail())
                .isActive(u.getIsActive())
                .roles(u.getRoles().stream().map(BaseNamedEntity::getName).toList())
                .createdAt(u.getCreatedAt())
                .build()
        );

        return new PagedResult<>(response);
    }

    @Override
    @Transactional
    @Auditable(action = "CREATE_OAUTH_USER")
    @CacheEvict(value = "usersList", allEntries = true)
    public String createOAuthUser(String email) {
        String baseUserName = email.split("@")[0];
        int counter = 1;

        while (_userRepo.existsByUsername(baseUserName.toUpperCase().trim())) {
            baseUserName = baseUserName + counter;
            counter++;
        }

        String randomPassword = UUID.randomUUID() + "G00G1E#";

        String ROLE_CLIENT = "ROLE_CLIENT";
        Roles role = _roleRepository.setRole(ROLE_CLIENT);

        Users user = new Users();
        user.setUsername(baseUserName);
        user.setNormalizedUsername(baseUserName.trim().toUpperCase());
        user.setEmail(email);
        user.setNormalizedEmail(email.toUpperCase().trim());
        user.setPassword(_passwordEncoder.encode(randomPassword));
        user.setIsActive(true);
        user.setRoles(Set.of(role));

        _userRepo.save(user);

        return baseUserName;
    }

    @Override
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

        return Generate2faResponse.builder()
                .qrCodeUri(qrUriCode)
                .manualCode(secret)
                .build();
    }

    @Override
    @Transactional
    @Auditable(action = "ENABLE_2FA")
    public void verifyAndEnable2fa(String userToken, Verify2faRequest request) {
        Users user = _userRepo.findByToken(userToken);

        if (user.getIsTwoFactorEnabled() != null && user.getIsTwoFactorEnabled())
            throw new IllegalStateException("2FA is already enabled");

        if (user.getMfaSecret() == null)
            throw new IllegalStateException("2FA secret is not generated yet. Please call generate first.");

        boolean isValid = _2faServices.isOptValid(user.getMfaSecret(), request.getCode());

        if (!isValid) throw new IllegalStateException("Invalid 2FA code. Try again.");

        user.setIsTwoFactorEnabled(true);
        _userRepo.save(user);
    }

    private void deleteAccount(String userToken) {
        Users user = _userRepo.findByToken(userToken);

        user.setNormalizedEmail(SoftDeleteHelpers.markAsDelete(user.getNormalizedEmail()));
        user.setNormalizedUsername(SoftDeleteHelpers.markAsDelete(user.getNormalizedUsername()));

        user.setEmail(SoftDeleteHelpers.markAsDelete(user.getEmail()));
        user.setUsername(SoftDeleteHelpers.markAsDelete(user.getUsername()));
        user.setIsActive(false);
        user.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        _userRepo.save(user);
    }

    private Users buildAndSaveUser(RegisterRequest request, String userRole, boolean isActive) {
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
        return user;
    }

    private UserListResponse createPayload(Users user) {
        return UserListResponse.builder()
                .token(user.getToken())
                .username(user.getUsername())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream().map(BaseNamedEntity::getName).toList())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
