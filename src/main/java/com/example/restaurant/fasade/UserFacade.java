package com.example.restaurant.fasade;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.Generate2faResponse;
import com.example.restaurant.fasade.interfaces.IUserFacade;
import com.example.restaurant.services.user.EmployeeManagementService;
import com.example.restaurant.services.user.UserAccountService;
import com.example.restaurant.services.user.UserIdentityService;
import com.example.restaurant.services.user.UserSecurityService;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFacade implements IUserFacade {
  private final EmployeeManagementService _employee;
  private final UserAccountService _userAccount;
  private final UserIdentityService _userIdentity;
  private final UserSecurityService _userSecurity;

  @Override
  @Transactional
  @CacheEvict(value = "usersList", allEntries = true)
  public String create(RegisterRequest request, String userRole, boolean isActive) {
    return _userIdentity.create(request, userRole, isActive);
  }

  @Override
  @Transactional
  @CacheEvict(value = "usersList", allEntries = true)
  public void activeUser(String userToken) {
    _userAccount.activeUser(userToken);
  }

  @Override
  public Optional<UserDomain> findMinimalByEmail(String email) {
    return _userIdentity.findMinimalByEmail(email);
  }

  @Override
  @Auditable(action = "UPDATE_PASSWORD")
  @Transactional
  public void updatePassword(String userToken, ChangePasswordRequest request) {
    _userSecurity.updatePassword(userToken, request);
  }

  @Override
  @Transactional
  public void changePassword(String token, String newPassword) {
    _userSecurity.changePassword(token, newPassword);
  }

  @Override
  @Auditable(action = "UPDATE_EMAIL")
  @Transactional
  public void updateEmail(String userToken, String email) {
    _userIdentity.updateEmail(userToken, email);
  }

  @Override
  @Transactional
  @Auditable(action = "CONFIRM_EMAIL_CHANGE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void confirmEmailChange(String userToken, String token) {
    _userIdentity.confirmEmailChange(userToken, token);
  }

  @Override
  @Transactional
  @Auditable(action = "UPDATE_USERNAME")
  @CacheEvict(value = "usersList", allEntries = true)
  public void updateUserName(String userName, String userToken) {
    _userAccount.updateUserName(userName, userToken);
  }

  @Override
  @Transactional
  @Auditable(action = "DELETE_ACCOUNT")
  @CacheEvict(value = "usersList", allEntries = true)
  public void delete(String userToken) {
    _userAccount.delete(userToken);
  }

  @Override
  @Transactional
  @Auditable(action = "ADD_NEW_EMPLOYEE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void createEmployee(AddEmployeeRequest request) {
    _employee.createEmployee(request);
  }

  @Override
  @Transactional
  @Auditable(action = "EDIT_EMPLOYEE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void editEmployee(EditEmployeeRequest request) {
    _employee.editEmployee(request);
  }

  @Override
  @Transactional
  @Auditable(action = "CHANGE_EMPLOYEE_PASSWORD")
  public void changeEmployeePassword(String adminToken, ChangeEmployeePasswordRequest request) {
    _employee.changeEmployeePassword(adminToken, request);
  }

  @Override
  @Transactional
  @Auditable(action = "CHANGE_EMPLOYEE_ROLE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void changeEmployeeRole(String adminToken, ChangeEmployeeRoleRequest request) {
    _employee.changeEmployeeRole(adminToken, request);
  }

  @Override
  @Transactional
  @Auditable(action = "CHANGE_EMPLOYEE_AVALAIBLE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void blockEmployee(String adminToken, BlockEmployeeRequest request) {
    _employee.blockEmployee(adminToken, request);
  }

  @Override
  @Transactional
  @Auditable(action = "DELETE_EMPLOYEE")
  @CacheEvict(value = "usersList", allEntries = true)
  public void deleteEmployee(String adminToken, String employeeToken) {
    _employee.deleteEmployee(adminToken, employeeToken);
  }

  @Override
  @Transactional
  @Auditable(action = "CREATE_OAUTH_USER")
  @CacheEvict(value = "usersList", allEntries = true)
  public String createOAuthUser(String email) {
    return _userAccount.createOAuthUser(email);
  }

  @Override
  @Transactional
  @Auditable(action = "GENERATE_2FA_SECRET")
  public Generate2faResponse generate2fa(String userToken) {
    return _userSecurity.generate2fa(userToken);
  }

  @Override
  @Transactional
  @Auditable(action = "ENABLE_2FA")
  public void verifyAndEnable2fa(String userToken, Verify2faRequest request) {
    _userSecurity.verifyAndEnable2fa(userToken, request);
  }
}
