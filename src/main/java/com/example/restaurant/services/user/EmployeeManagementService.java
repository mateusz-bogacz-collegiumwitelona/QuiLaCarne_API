package com.example.restaurant.services.user;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.helpers.UserManagmentHelper;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeManagementService {
  private final IUserRepository _userRepo;
  private final IRoleRepository _roleRepository;
  private final PasswordEncoder _passwordEncoder;
  private final UserManagmentHelper _userHelper;
  private final UserSyncPublisher _userPublisher;

  private static final String ROLE_MANAGER = "ROLE_MANAGER";
  private static final String ROLE_WAITER = "ROLE_WAITER";

  @Transactional
  @Auditable(action = "ADD_NEW_EMPLOYEE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void createEmployee(AddEmployeeRequest request) {
    Users employee;
    if (request.isAdmin()) {
      employee = _userHelper.buildAndSaveUser(request.getRegister(), ROLE_MANAGER, true);
    } else {
      employee = _userHelper.buildAndSaveUser(request.getRegister(), ROLE_WAITER, true);
    }

    _userPublisher.publishUserCreated(employee);
    log.info("Created employee {}", employee.getUsername());
  }

  @Transactional
  @Auditable(action = "EDIT_EMPLOYEE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void editEmployee(EditEmployeeRequest request) {
    Users employee = _userRepo.findByToken(request.getEmployeeToken());

    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      changeEmail(employee, request.getEmail());
    }

    if (request.getUserName() != null && !request.getUserName().isBlank()) {
      _userHelper.changeUsername(employee, request.getUserName());
    }

    _userRepo.save(employee);

    _userPublisher.publishUserUpdated(employee);
    log.info("Updated employee {}", employee.getUsername());
  }

  @Transactional
  @Auditable(action = "CHANGE_EMPLOYEE_ROLE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void changeEmployeeRole(String adminToken, ChangeEmployeeRoleRequest request) {
    if (request.getEmployeeToken().trim().equals(adminToken.trim()))
      throw new IllegalStateException("You can't change your own role");

    Users user = _userRepo.findByToken(request.getEmployeeToken());

    boolean isCurrentlyManager =
        user.getRoles().stream().anyMatch(r -> ROLE_MANAGER.equals(r.getName()));

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

    _userPublisher.publishUserUpdated(user);
    log.info("Updated employee role {}", user.getUsername());
  }

  @Transactional
  @Auditable(action = "CHANGE_EMPLOYEE_AVALAIBLE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void blockEmployee(String adminToken, BlockEmployeeRequest request) {
    if (adminToken.trim().equals(request.getEmployeeToken().trim()))
      throw new IllegalStateException("You can't change your own availability");

    Users user = _userRepo.findByToken(request.getEmployeeToken());

    if (request.isAvailable() == Boolean.TRUE.equals(user.getIsActive()))
      throw new IllegalStateException("Employee availability is already set to this state");

    user.setIsActive(request.isAvailable());

    _userRepo.save(user);

    _userPublisher.publishUserUpdated(user);
    log.info("Blocked employee {}", user.getUsername());
  }

  @Transactional
  @Auditable(action = "DELETE_EMPLOYEE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void deleteEmployee(String adminToken, String employeeToken) {
    if (adminToken.trim().equals(employeeToken.trim()))
      throw new IllegalStateException("You can't delete yourself");

    _userHelper.deleteAccount(employeeToken);

    _userPublisher.publishUserDeleted(employeeToken);
    log.info("Deleted employee {}", employeeToken);
  }

  @Transactional
  @Auditable(action = "CHANGE_EMPLOYEE_PASSWORD")
  public void changeEmployeePassword(String adminToken, ChangeEmployeePasswordRequest request) {
    if (request.getEmployeeToken().trim().equals(adminToken.trim()))
      throw new IllegalStateException("You can't change your own password");

    if (!request.getPassword().equals(request.getConfirmPassword()))
      throw new IllegalStateException("Passwords do not match");

    Users user = _userRepo.findByToken(request.getEmployeeToken());

    user.setPassword(_passwordEncoder.encode(request.getConfirmPassword()));

    _userRepo.save(user);
    _userPublisher.publishUserUpdated(user);
    log.info("Changed employee password {}", user.getUsername());
  }

  private void changeEmail(Users user, String email) {
    String normalizedEmail = email.toUpperCase().trim();

    if (normalizedEmail.equals(user.getNormalizedEmail()))
      throw new IllegalStateException("Email must be different");

    if (_userRepo.findByNormalizedEmail(normalizedEmail).isPresent())
      throw new EntityAlreadyExistsException("User with this email already exists");

    user.setEmail(email.trim());
    user.setNormalizedEmail(normalizedEmail);
  }
}
