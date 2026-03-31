package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.InvalidDateException;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServicesTest {
    @Mock
    private IUserRepository _userRepo;

    @Mock
    private IRoleRepository _roleRepository;
    @Mock
    private PasswordEncoder _passwordEncoder;
    @Mock
    private IVerificationTokenServices _tokenServices;

    @InjectMocks
    private UserServices _userServices;

    @Test
    @DisplayName("Create User: Success")
    void create_ShouldReturnToken_WhenDataIsValid() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(TestConstants.FAKE_USERNAME);
        request.setEmail(TestConstants.FAKE_EMAIL);
        request.setPassword(TestConstants.FAKE_PASSWORD);

        Roles mockRole = new Roles();
        when(_userRepo.findByNormalizedEmail(anyString())).thenReturn(Optional.empty());
        when(_userRepo.existsByUsername(anyString())).thenReturn(false);
        when(_roleRepository.setRole(anyString())).thenReturn(mockRole);
        when(_passwordEncoder.encode(anyString())).thenReturn(TestConstants.FAKE_HASH);

        doAnswer(invocation -> {
            Users user = invocation.getArgument(0);
            user.setToken(TestConstants.FAKE_USER_TOKEN);
            return null;
        }).when(_userRepo).save(any(Users.class));

        String token = _userServices.create(request, "ROLE_CLIENT", false);

        assertEquals(TestConstants.FAKE_USER_TOKEN, token);
        verify(_userRepo).save(any(Users.class));
    }

    @Test
    @DisplayName("Active User: Success")
    void activeUser_ShouldSucceed_WhenUserInactive() {
        Users user = new Users();
        user.setIsActive(false);
        when(_userRepo.findByToken("token")).thenReturn(user);

        _userServices.activeUser("token");

        assertTrue(user.getIsActive());
        verify(_userRepo).save(user);
    }

    @Test
    @DisplayName("Active User: Throws IllegalStateException if already active")
    void activeUser_ShouldThrowException_WhenUserAlreadyActive() {
        Users user = new Users();
        user.setIsActive(true);
        when(_userRepo.findByToken(anyString())).thenReturn(user);

        assertThrows(IllegalStateException.class, () -> _userServices.activeUser(TestConstants.FAKE_USER_TOKEN));
    }

    @Test
    @DisplayName("Update Password: Throws BadCredentialsException on wrong old password")
    void updatePassword_ShouldThrowException_WhenOldPasswordInvalid() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong");
        request.setPassword("new");
        request.setConfirmPassword("new");

        Users user = new Users();
        user.setPassword("hashed_old");

        when(_userRepo.findByToken(anyString())).thenReturn(user);
        when(_passwordEncoder.matches("wrong", "hashed_old")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> _userServices.updatePassword(TestConstants.FAKE_USER_TOKEN, request));
    }

    @Test
    @DisplayName("Update Password: Throws Exception if user want change email to current email")
    void updateEmail_ShouldThrowException_WhenSameEmailAsCurrent() {
        Users user = new Users();
        user.setNormalizedEmail("TEST@TEST.PL");
        when(_userRepo.findByToken(anyString())).thenReturn(user);

        assertThrows(IllegalStateException.class, () ->
                _userServices.updateEmail("token", "test@test.pl"));
    }

    @Test
    @DisplayName("Update Email: Throws EntityAlreadyExistsException if email taken")
    void updateEmail_ShouldThrowException_WhenEmailTakenByAnother() {
        Users currentUser = new Users();
        currentUser.setToken(TestConstants.FAKE_USER_TOKEN);

        Users otherUser = new Users();
        otherUser.setToken("different_token");

        when(_userRepo.findByNormalizedEmail(anyString())).thenReturn(Optional.of(otherUser));

        assertThrows(EntityAlreadyExistsException.class, () -> _userServices.updateEmail(TestConstants.FAKE_USER_TOKEN, "taken@test.pl"));
    }

    @Test
    @DisplayName("Confirm Email Change: Throws InvalidDateException on bad token")
    void confirmEmailChange_ShouldThrowException_WhenTokenInvalid() {
        when(_tokenServices.validateToken(anyString(), anyString(), any())).thenReturn(false);

        assertThrows(InvalidDateException.class, () -> _userServices.confirmEmailChange(TestConstants.FAKE_USER_TOKEN, "bad_token"));
    }


    @Test
    @DisplayName("Update Username: Success")
    void updateUserName_ShouldUpdate_WhenValid() {
        Users user = new Users();
        user.setNormalizedUsername("OLD_NAME");
        when(_userRepo.findByToken(anyString())).thenReturn(user);
        when(_userRepo.existsByUsername(anyString())).thenReturn(false);

        _userServices.updateUserName("NewName", TestConstants.FAKE_USER_TOKEN);

        assertEquals("NewName", user.getUsername());
        verify(_userRepo).save(user);
    }

    @Test
    @DisplayName("Update Username: Throw Exception when name is same as current")
    void updateUserName_ShouldThrowException_WhenNameIsSameAsCurrent() {
        Users user = new Users();
        user.setNormalizedUsername("MATI");
        when(_userRepo.findByToken(anyString())).thenReturn(user);

        assertThrows(IllegalStateException.class, () ->
                _userServices.updateUserName("Mati", "token"));
    }

    @Test
    @DisplayName("Delete Account: Success with Soft Delete")
    void deleteAccount_ShouldAnonymizeDataAndDeactivate() {
        Users user = new Users();
        user.setUsername("Mati");
        user.setEmail("mati@test.pl");
        when(_userRepo.findByToken(anyString())).thenReturn(user);

        _userServices.deleteAccount(TestConstants.FAKE_USER_TOKEN);

        assertFalse(user.getIsActive());
        assertTrue(user.getUsername().startsWith("DELETED_"));
        assertNotNull(user.getDeletedAt());
        verify(_userRepo).save(user);
    }

    @Test
    @DisplayName("Create User: Throws EntityAlreadyExistsException when email is taken")
    void create_ShouldThrowException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(TestConstants.FAKE_USERNAME);
        request.setEmail(TestConstants.FAKE_EMAIL);
        request.setPassword(TestConstants.FAKE_PASSWORD);

        when(_userRepo.findByNormalizedEmail(anyString())).thenReturn(Optional.of(new Users()));

        assertThrows(EntityAlreadyExistsException.class, () -> _userServices.create(request, "ROLE_CLIENT", false));
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Create User: Throws EntityAlreadyExistsException when username is taken")
    void create_ShouldThrowException_WhenUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(TestConstants.FAKE_USERNAME);
        request.setEmail(TestConstants.FAKE_EMAIL);
        request.setPassword(TestConstants.FAKE_PASSWORD);

        when(_userRepo.findByNormalizedEmail(anyString())).thenReturn(Optional.empty());
        when(_userRepo.existsByUsername(anyString())).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> _userServices.create(request, "ROLE_CLIENT", false));
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Create Employee: Success for Admin role")
    void createEmployee_ShouldCreateManager_WhenAdminIsTrue() {
        AddEmployeeRequest request = new AddEmployeeRequest();
        request.setAdmin(true);
        RegisterRequest regRequest = new RegisterRequest();
        regRequest.setUsername("Manager1");
        regRequest.setEmail("manager@test.pl");
        regRequest.setPassword("Pass123!");
        request.setRegister(regRequest);

        when(_userRepo.findByNormalizedEmail(anyString())).thenReturn(Optional.empty());
        when(_userRepo.existsByUsername(anyString())).thenReturn(false);
        when(_roleRepository.setRole("ROLE_MANAGER")).thenReturn(new Roles());

        _userServices.createEmployee(request);

        verify(_roleRepository).setRole("ROLE_MANAGER");
        verify(_userRepo).save(any(Users.class));
    }

    @Test
    @DisplayName("Create Employee: Success for Waiter role")
    void createEmployee_ShouldCreateWaiter_WhenAdminIsFalse() {
        AddEmployeeRequest request = new AddEmployeeRequest();
        request.setAdmin(false);
        RegisterRequest regRequest = new RegisterRequest();
        regRequest.setUsername("Waiter1");
        regRequest.setEmail("waiter@test.pl");
        regRequest.setPassword("Pass123!");
        request.setRegister(regRequest);

        when(_userRepo.findByNormalizedEmail(anyString())).thenReturn(Optional.empty());
        when(_userRepo.existsByUsername(anyString())).thenReturn(false);
        when(_roleRepository.setRole("ROLE_WAITER")).thenReturn(new Roles());

        _userServices.createEmployee(request);

        verify(_roleRepository).setRole("ROLE_WAITER");
        verify(_userRepo).save(any(Users.class));
    }

    @Test
    @DisplayName("Edit Employee: Success when updating both email and username")
    void editEmployee_ShouldUpdateBoth_WhenValid() {
        EditEmployeeRequest request = new EditEmployeeRequest();
        request.setEmployeeToken(TestConstants.FAKE_USER_TOKEN);
        request.setEmail("new_employee@test.pl");
        request.setUserName("NewEmployeeName");

        Users employee = new Users();
        employee.setEmail("old@test.pl");
        employee.setNormalizedEmail("OLD@TEST.PL");
        employee.setUsername("OldName");
        employee.setNormalizedUsername("OLDNAME");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(employee);
        when(_userRepo.findByNormalizedEmail("NEW_EMPLOYEE@TEST.PL")).thenReturn(Optional.empty());
        when(_userRepo.existsByUsername("NEWEMPLOYEENAME")).thenReturn(false);

        _userServices.editEmployee(request);

        assertEquals("new_employee@test.pl", employee.getEmail());
        assertEquals("NEW_EMPLOYEE@TEST.PL", employee.getNormalizedEmail());
        assertEquals("NewEmployeeName", employee.getUsername());
        assertEquals("NEWEMPLOYEENAME", employee.getNormalizedUsername());
        verify(_userRepo).save(employee);
    }

    @Test
    @DisplayName("Edit Employee: Success when updating only email")
    void editEmployee_ShouldUpdateOnlyEmail_WhenUsernameIsBlank() {
        EditEmployeeRequest request = new EditEmployeeRequest();
        request.setEmployeeToken(TestConstants.FAKE_USER_TOKEN);
        request.setEmail("new_employee@test.pl");
        request.setUserName("   ");

        Users employee = new Users();
        employee.setNormalizedEmail("OLD@TEST.PL");
        employee.setUsername("OldName");
        employee.setNormalizedUsername("OLDNAME");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(employee);
        when(_userRepo.findByNormalizedEmail("NEW_EMPLOYEE@TEST.PL")).thenReturn(Optional.empty());

        _userServices.editEmployee(request);

        assertEquals("new_employee@test.pl", employee.getEmail());
        assertEquals("OldName", employee.getUsername(), "Username should not change");
        verify(_userRepo).save(employee);
    }

    @Test
    @DisplayName("Edit Employee: Throws IllegalStateException when new email is the same as current")
    void editEmployee_ShouldThrowException_WhenEmailIsSameAsCurrent() {
        EditEmployeeRequest request = new EditEmployeeRequest();
        request.setEmployeeToken(TestConstants.FAKE_USER_TOKEN);
        request.setEmail("same@test.pl");

        Users employee = new Users();
        employee.setNormalizedEmail("SAME@TEST.PL");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(employee);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> _userServices.editEmployee(request));
        assertEquals("Email must be different", exception.getMessage());
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Edit Employee: Throws EntityAlreadyExistsException when new email is already taken")
    void editEmployee_ShouldThrowException_WhenEmailIsTaken() {
        EditEmployeeRequest request = new EditEmployeeRequest();
        request.setEmployeeToken(TestConstants.FAKE_USER_TOKEN);
        request.setEmail("taken@test.pl");

        Users employee = new Users();
        employee.setNormalizedEmail("OLD@TEST.PL");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(employee);
        when(_userRepo.findByNormalizedEmail("TAKEN@TEST.PL")).thenReturn(Optional.of(new Users()));

        assertThrows(EntityAlreadyExistsException.class, () -> _userServices.editEmployee(request));
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Edit Employee: Throws IllegalStateException when new username is the same as current")
    void editEmployee_ShouldThrowException_WhenUsernameIsSameAsCurrent() {
        EditEmployeeRequest request = new EditEmployeeRequest();
        request.setEmployeeToken(TestConstants.FAKE_USER_TOKEN);
        request.setUserName("SameName");

        Users employee = new Users();
        employee.setNormalizedUsername("SAMENAME");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(employee);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> _userServices.editEmployee(request));
        assertEquals("User name must be different", exception.getMessage());
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Edit Employee: Throws EntityAlreadyExistsException when new username is already taken")
    void editEmployee_ShouldThrowException_WhenUsernameIsTaken() {
        EditEmployeeRequest request = new EditEmployeeRequest();
        request.setEmployeeToken(TestConstants.FAKE_USER_TOKEN);
        request.setUserName("TakenName");

        Users employee = new Users();
        employee.setNormalizedUsername("OLDNAME");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(employee);
        when(_userRepo.existsByUsername("TAKENNAME")).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> _userServices.editEmployee(request));
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Change Employee Password: Success when valid data is provided")
    void changeEmployeePassword_ShouldSucceed_WhenValidData() {
        String adminToken = "ADMIN_TOKEN_123";
        ChangeEmployeePasswordRequest request = new ChangeEmployeePasswordRequest();
        request.setEmployeeToken("EMPLOYEE_TOKEN_456");
        request.setPassword("NewPass123!");
        request.setConfirmPassword("NewPass123!");

        Users employee = new Users();
        employee.setToken("EMPLOYEE_TOKEN_456");
        employee.setPassword("OldHashedPassword");

        when(_userRepo.findByToken("EMPLOYEE_TOKEN_456")).thenReturn(employee);
        when(_passwordEncoder.encode("NewPass123!")).thenReturn("NewHashedPassword123");

        _userServices.changeEmployeePassword(adminToken, request);

        assertEquals("NewHashedPassword123", employee.getPassword());
        verify(_userRepo, times(1)).save(employee);
    }

    @Test
    @DisplayName("Change Employee Password: Throws IllegalStateException when manager tries to change own password")
    void changeEmployeePassword_ShouldThrowException_WhenAdminChangesOwnPassword() {
        String adminToken = "ADMIN_TOKEN_123";
        ChangeEmployeePasswordRequest request = new ChangeEmployeePasswordRequest();
        request.setEmployeeToken(" ADMIN_TOKEN_123 ");
        request.setPassword("NewPass123!");
        request.setConfirmPassword("NewPass123!");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> _userServices.changeEmployeePassword(adminToken, request));

        assertEquals("You can't change your own password", exception.getMessage());
        verify(_userRepo, never()).save(any());
        verify(_passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Change Employee Password: Throws IllegalStateException when passwords do not match")
    void changeEmployeePassword_ShouldThrowException_WhenPasswordsMismatch() {
        String adminToken = "ADMIN_TOKEN_123";
        ChangeEmployeePasswordRequest request = new ChangeEmployeePasswordRequest();
        request.setEmployeeToken("EMPLOYEE_TOKEN_456");
        request.setPassword("NewPass123!");
        request.setConfirmPassword("DifferentPass123!");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> _userServices.changeEmployeePassword(adminToken, request));

        assertEquals("Passwords do not match", exception.getMessage());
        verify(_userRepo, never()).save(any());
        verify(_passwordEncoder, never()).encode(anyString());
    }
}