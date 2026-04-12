package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.SyncUserResponse;
import com.example.restaurant.enums.WebSocketEventType;
import com.example.restaurant.exceptions.EntityAlreadyExistsException;
import com.example.restaurant.exceptions.InvalidDateException;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IRoleRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IVerificationTokenServices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

    @Mock
    private TwoFactorServices _2faServices;

    @Mock
    private NotificationServices _notification;

    @InjectMocks
    private UserServices _userServices;

    @Spy
    private SyncMapper _syncMapper = Mappers.getMapper(SyncMapper.class);

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

        assertThrows(BadCredentialsException.class, () -> _userServices
                .updatePassword(TestConstants.FAKE_USER_TOKEN, request));
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

        assertThrows(EntityAlreadyExistsException.class, () -> _userServices
                .updateEmail(TestConstants.FAKE_USER_TOKEN, "taken@test.pl"));
    }

    @Test
    @DisplayName("Confirm Email Change: Throws InvalidDateException on bad token")
    void confirmEmailChange_ShouldThrowException_WhenTokenInvalid() {
        when(_tokenServices.validateToken(anyString(), anyString(), any())).thenReturn(false);

        assertThrows(InvalidDateException.class, () -> _userServices
                .confirmEmailChange(TestConstants.FAKE_USER_TOKEN, "bad_token"));
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
    void delete_ShouldAnonymizeDataAndDeactivate() {
        Users user = new Users();
        user.setUsername("Mati");
        user.setEmail("mati@test.pl");
        user.setNormalizedUsername("MATI");
        user.setNormalizedEmail("MATI@TEST.PL");

        when(_userRepo.findByToken(anyString())).thenReturn(user);

        _userServices.delete(TestConstants.FAKE_USER_TOKEN);

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

        assertThrows(EntityAlreadyExistsException.class, () -> _userServices
                .create(request, "ROLE_CLIENT", false));
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

        assertThrows(EntityAlreadyExistsException.class, () -> _userServices
                .create(request, "ROLE_CLIENT", false));
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

        Roles managerRole = new Roles();
        managerRole.setName("ROLE_MANAGER");
        managerRole.setToken("ROLE_MANAGER_TOKEN"); // Dodajemy token do mocka
        when(_roleRepository.setRole("ROLE_MANAGER")).thenReturn(managerRole);

        doAnswer(invocation -> {
            Users u = invocation.getArgument(0);
            u.setToken("NEW_MANAGER_TOKEN");
            return u;
        }).when(_userRepo).save(any(Users.class));

        _userServices.createEmployee(request);

        verify(_roleRepository).setRole("ROLE_MANAGER");
        verify(_userRepo).save(any(Users.class));

        verify(_notification, times(1)).sendEventToTopic(
                eq("/personnel/updates"),
                argThat(event ->
                        event != null &&
                                event.getEventType() == WebSocketEventType.CREATED &&
                                "EMPLOYEE".equals(event.getEntityType()) &&
                                "NEW_MANAGER_TOKEN".equals(event.getToken()) &&
                                event.getPayload() != null &&
                                "manager@test.pl".equals(((SyncUserResponse) event.getPayload()).getEmail())
                )
        );
    }

    @Test
    @DisplayName("Edit Employee: Success when updating both email and username")
    void editEmployee_ShouldUpdateBoth_WhenValid() {
        EditEmployeeRequest request = new EditEmployeeRequest();
        request.setEmployeeToken(TestConstants.FAKE_USER_TOKEN);
        request.setEmail("new_employee@test.pl");
        request.setUserName("NewEmployeeName");

        Users employee = new Users();
        employee.setToken(TestConstants.FAKE_USER_TOKEN);
        employee.setEmail("old@test.pl");
        employee.setNormalizedEmail("OLD@TEST.PL");
        employee.setUsername("OldName");
        employee.setNormalizedUsername("OLDNAME");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(employee);
        when(_userRepo.findByNormalizedEmail("NEW_EMPLOYEE@TEST.PL")).thenReturn(Optional.empty());
        when(_userRepo.existsByUsername("NEWEMPLOYEENAME")).thenReturn(false);

        _userServices.editEmployee(request);

        assertEquals("new_employee@test.pl", employee.getEmail());
        verify(_userRepo).save(employee);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/personnel/updates"),
                argThat(event ->
                        event != null &&
                                event.getEventType() == WebSocketEventType.UPDATED &&
                                "EMPLOYEE".equals(event.getEntityType()) &&
                                TestConstants.FAKE_USER_TOKEN.equals(event.getToken()) &&
                                event.getPayload() != null &&
                                "new_employee@test.pl".equals(((SyncUserResponse) event.getPayload()).getEmail())
                )
        );
    }

    @Test
    @DisplayName("Edit Employee: Success when updating only email")
    void editEmployee_ShouldUpdateOnlyEmail_WhenUsernameIsBlank() {
        EditEmployeeRequest request = new EditEmployeeRequest();
        request.setEmployeeToken(TestConstants.FAKE_USER_TOKEN);
        request.setEmail("new_employee@test.pl");
        request.setUserName("   ");

        Users employee = new Users();
        employee.setToken(TestConstants.FAKE_USER_TOKEN);
        employee.setNormalizedEmail("OLD@TEST.PL");
        employee.setUsername("OldName");
        employee.setNormalizedUsername("OLDNAME");

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(employee);
        when(_userRepo.findByNormalizedEmail("NEW_EMPLOYEE@TEST.PL")).thenReturn(Optional.empty());

        _userServices.editEmployee(request);

        verify(_userRepo).save(employee);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/personnel/updates"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.UPDATED &&
                                event.getEntityType().equals("EMPLOYEE") &&
                                event.getPayload() != null
                )
        );
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

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> _userServices
                .editEmployee(request));
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

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> _userServices
                .editEmployee(request));
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

    @Test
    @DisplayName("Change Employee Role: Throws Exception when admin tries to change own role")
    void changeEmployeeRole_ShouldThrowException_WhenAdminChangesOwnRole() {
        ChangeEmployeeRoleRequest request = new ChangeEmployeeRoleRequest();
        request.setEmployeeToken("admin-token");
        request.setAdmin(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> _userServices.changeEmployeeRole("admin-token", request));

        assertEquals("You can't change your own role", exception.getMessage());
        verify(_userRepo, never()).save(any());
        verify(_roleRepository, never()).setRole(anyString());
    }

    @Test
    @DisplayName("Change Employee Role: Throws Exception when user is already a Manager")
    void changeEmployeeRole_ShouldThrowException_WhenAlreadyManager() {
        ChangeEmployeeRoleRequest request = new ChangeEmployeeRoleRequest();
        request.setEmployeeToken("employee-token");
        request.setAdmin(true);

        Roles managerRole = new Roles();
        managerRole.setName("ROLE_MANAGER");

        Users employee = new Users();
        employee.setRoles(Set.of(managerRole));
        when(_userRepo.findByToken("employee-token")).thenReturn(employee);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> _userServices.changeEmployeeRole("admin-token", request));

        assertEquals("User is already a Manager", exception.getMessage());
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Change Employee Role: Throws Exception when user is already a Waiter")
    void changeEmployeeRole_ShouldThrowException_WhenAlreadyWaiter() {
        ChangeEmployeeRoleRequest request = new ChangeEmployeeRoleRequest();
        request.setEmployeeToken("employee-token");
        request.setAdmin(false);

        Roles waiterRole = new Roles();
        waiterRole.setName("ROLE_WAITER");

        Users employee = new Users();
        employee.setRoles(Set.of(waiterRole));

        when(_userRepo.findByToken("employee-token")).thenReturn(employee);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> _userServices.changeEmployeeRole("admin-token", request));

        assertEquals("User is already a Waiter", exception.getMessage());
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Change Employee Role: Success promoting to Manager")
    void changeEmployeeRole_ShouldPromoteToManager_WhenValid() {
        ChangeEmployeeRoleRequest request = new ChangeEmployeeRoleRequest();
        request.setEmployeeToken("employee-token");
        request.setAdmin(true);

        Roles waiterRole = new Roles();
        waiterRole.setName("ROLE_WAITER");
        waiterRole.setToken("ROLE_WAITER_TOKEN");

        Users employee = new Users();
        employee.setToken("employee-token");
        employee.setRoles(new HashSet<>(Set.of(waiterRole)));

        Roles newManagerRole = new Roles();
        newManagerRole.setName("ROLE_MANAGER");
        newManagerRole.setToken("ROLE_MANAGER_TOKEN");

        when(_userRepo.findByToken("employee-token")).thenReturn(employee);
        when(_roleRepository.setRole("ROLE_MANAGER")).thenReturn(newManagerRole);

        _userServices.changeEmployeeRole("admin-token", request);

        assertTrue(employee.getRoles().contains(newManagerRole));
        verify(_userRepo).save(employee);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/personnel/updates"),
                argThat(event ->
                        event != null &&
                                event.getEventType() == WebSocketEventType.UPDATED &&
                                "EMPLOYEE".equals(event.getEntityType()) &&
                                "employee-token".equals(event.getToken()) &&
                                event.getPayload() != null &&
                                ((SyncUserResponse) event.getPayload()).getRoleTokens().contains("ROLE_MANAGER_TOKEN")
                )
        );
    }

    @Test
    @DisplayName("Change Employee Role: Success demoting to Waiter")
    void changeEmployeeRole_ShouldDemoteToWaiter_WhenValid() {
        ChangeEmployeeRoleRequest request = new ChangeEmployeeRoleRequest();
        request.setEmployeeToken("employee-token");
        request.setAdmin(false);

        Roles managerRole = new Roles();
        managerRole.setName("ROLE_MANAGER");
        managerRole.setToken("ROLE_MANAGER_TOKEN");

        Users employee = new Users();
        employee.setToken("employee-token");
        employee.setRoles(new HashSet<>(Set.of(managerRole)));

        Roles newWaiterRole = new Roles();
        newWaiterRole.setName("ROLE_WAITER");
        newWaiterRole.setToken("ROLE_WAITER_TOKEN");

        when(_userRepo.findByToken("employee-token")).thenReturn(employee);
        when(_roleRepository.setRole("ROLE_WAITER")).thenReturn(newWaiterRole);

        _userServices.changeEmployeeRole("admin-token", request);

        assertTrue(employee.getRoles().contains(newWaiterRole));
        verify(_userRepo).save(employee);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/personnel/updates"),
                argThat(event ->
                        event != null &&
                                event.getEventType() == WebSocketEventType.UPDATED &&
                                "EMPLOYEE".equals(event.getEntityType()) &&
                                event.getPayload() != null &&
                                ((SyncUserResponse) event.getPayload()).getRoleTokens().contains("ROLE_WAITER_TOKEN")
                )
        );
    }

    @Test
    @DisplayName("Change Employee Availability: Throws Exception when admin tries to change own availability")
    void blockEmployee_ShouldThrowException_WhenAdminChangesOwnAvailability() {
        BlockEmployeeRequest request = new BlockEmployeeRequest();
        request.setEmployeeToken("admin-token");
        request.setAvailable(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> _userServices.blockEmployee("admin-token", request));

        assertEquals("You can't change your own availability", exception.getMessage());
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Change Employee Availability: Throws Exception when status is already the same")
    void blockEmployee_ShouldThrowException_WhenAvailabilityIsTheSame() {
        BlockEmployeeRequest request = new BlockEmployeeRequest();
        request.setEmployeeToken("employee-token");
        request.setAvailable(false);

        Users employee = new Users();
        employee.setIsActive(false);

        when(_userRepo.findByToken("employee-token")).thenReturn(employee);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> _userServices.blockEmployee("admin-token", request));

        assertEquals("Employee availability is already set to this state", exception.getMessage());
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Change Employee Availability: Success when blocking an active employee")
    void blockEmployee_ShouldBlockEmployee_WhenValid() {
        BlockEmployeeRequest request = new BlockEmployeeRequest();
        request.setEmployeeToken("employee-token");
        request.setAvailable(false);

        Users employee = new Users();
        employee.setToken("employee-token");
        employee.setIsActive(true);

        when(_userRepo.findByToken("employee-token")).thenReturn(employee);

        _userServices.blockEmployee("admin-token", request);

        assertFalse(employee.getIsActive());
        verify(_userRepo).save(employee);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/personnel/updates"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.UPDATED &&
                                event.getEntityType().equals("EMPLOYEE") &&
                                "employee-token".equals(event.getToken()) &&
                                event.getPayload() != null &&
                                !((SyncUserResponse) event.getPayload()).getIsActive()
                )
        );
    }

    @Test
    @DisplayName("Change Employee Availability: Success when unblocking an inactive employee")
    void blockEmployee_ShouldUnblockEmployee_WhenValid() {
        BlockEmployeeRequest request = new BlockEmployeeRequest();
        request.setEmployeeToken("employee-token");
        request.setAvailable(true);

        Users employee = new Users();
        employee.setToken("employee-token");
        employee.setIsActive(false);

        when(_userRepo.findByToken("employee-token")).thenReturn(employee);

        _userServices.blockEmployee("admin-token", request);

        assertTrue(employee.getIsActive());
        verify(_userRepo).save(employee);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/personnel/updates"),
                argThat(event ->
                        event != null &&
                                event.getEventType() == WebSocketEventType.UPDATED &&
                                "EMPLOYEE".equals(event.getEntityType()) &&
                                event.getPayload() != null &&
                                ((SyncUserResponse) event.getPayload()).getIsActive()
                )
        );
    }

    @Test
    @DisplayName("Delete Employee: Throws Exception when admin tries to delete themselves")
    void deleteEmployee_ShouldThrowException_WhenDeletingSelf() {
        String adminToken = "admin-token";
        String employeeToken = " admin-token ";

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> _userServices.deleteEmployee(adminToken, employeeToken));

        assertEquals("You can't delete yourself", exception.getMessage());
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Delete Employee: Success with Soft Delete")
    void deleteEmployee_ShouldAnonymizeDataAndDeactivate() {
        String adminToken = "admin-token";
        String employeeToken = "employee-token";

        Users employee = new Users();
        employee.setToken("employee-token");
        employee.setUsername("Tomek");
        employee.setEmail("tomek@test.pl");
        employee.setNormalizedUsername("TOMEK");
        employee.setNormalizedEmail("TOMEK@TEST.PL");

        when(_userRepo.findByToken("employee-token")).thenReturn(employee);

        _userServices.deleteEmployee(adminToken, employeeToken);

        assertFalse(employee.getIsActive());
        verify(_userRepo).save(employee);

        verify(_notification, times(1)).sendEventToTopic(
                eq("/personnel/updates"),
                argThat(event ->
                        event.getEventType() == WebSocketEventType.DELETED &&
                                event.getEntityType().equals("EMPLOYEE") &&
                                "employee-token".equals(event.getToken()) &&
                                event.getPayload() == null
                )
        );
    }

    @Test
    @DisplayName("Create OAuth User: Success and generates unique username")
    void createOAuthUser_ShouldCreateUser_WithUniqueUsername() {
        String email = "jan.kowalski@gmail.com";

        when(_userRepo.existsByUsername("JAN.KOWALSKI")).thenReturn(true).thenReturn(false);

        Roles clientRole = new Roles();
        clientRole.setName("ROLE_CLIENT");
        when(_roleRepository.setRole("ROLE_CLIENT")).thenReturn(clientRole);
        when(_passwordEncoder.encode(anyString())).thenReturn("hashed_google_password");

        String generatedUsername = _userServices.createOAuthUser(email);

        assertEquals("jan.kowalski1", generatedUsername);

        verify(_userRepo).save(argThat(user ->
                user.getEmail().equals(email) &&
                        user.getNormalizedEmail().equals("JAN.KOWALSKI@GMAIL.COM") &&
                        user.getUsername().equals("jan.kowalski1") &&
                        user.getIsActive() &&
                        user.getPassword().equals("hashed_google_password") &&
                        user.getRoles().contains(clientRole)
        ));
    }

    @Test
    @DisplayName("Generate 2FA: Success")
    void generate2fa_ShouldReturnResponse_WhenValid() {
        String userToken = "user-token";
        Users user = new Users();
        user.setUsername("Mati");
        user.setIsTwoFactorEnabled(false);

        String fakeSecret = "JBOSSWS3DPEHPK3PXP";
        String fakeQrUri = "otpauth://totp/QuiLaCarne:Mati?secret=JBSWY3DPEHPK3PXP";

        when(_userRepo.findByToken(userToken)).thenReturn(user);
        when(_2faServices.generateNewSecret()).thenReturn(fakeSecret);
        when(_2faServices.generateQrCodeImageUri(fakeSecret, "Mati")).thenReturn(fakeQrUri);

        var response = _userServices.generate2fa(userToken);

        assertNotNull(response);
        assertEquals(fakeSecret, response.getManualCode());
        assertEquals(fakeQrUri, response.getQrCodeUri());
        assertEquals(fakeSecret, user.getMfaSecret());
        verify(_userRepo).save(user);
    }

    @Test
    @DisplayName("Generate 2FA: Throws Exception if already enabled")
    void generate2fa_ShouldThrowException_WhenAlreadyEnabled() {
        String userToken = "user-token";
        Users user = new Users();
        user.setIsTwoFactorEnabled(true);

        when(_userRepo.findByToken(userToken)).thenReturn(user);

        assertThrows(IllegalStateException.class, () -> _userServices.generate2fa(userToken));
        verify(_userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Verify and Enable 2FA: Success")
    void verifyAndEnable2fa_ShouldEnable_WhenCodeIsValid() {
        String userToken = "user-token";
        Verify2faRequest request = new Verify2faRequest();
        request.setCode(123456);

        Users user = new Users();
        user.setIsTwoFactorEnabled(false);
        user.setMfaSecret("SECRET");

        when(_userRepo.findByToken(userToken)).thenReturn(user);
        when(_2faServices.isOptValid("SECRET", 123456)).thenReturn(true);

        _userServices.verifyAndEnable2fa(userToken, request);

        assertTrue(user.getIsTwoFactorEnabled());
        verify(_userRepo).save(user);
    }

    @Test
    @DisplayName("Verify and Enable 2FA: Throws Exception on invalid code")
    void verifyAndEnable2fa_ShouldThrowException_WhenCodeIsInvalid() {
        String userToken = "user-token";
        Verify2faRequest request = new Verify2faRequest();
        request.setCode(999999);

        Users user = new Users();
        user.setMfaSecret("SECRET");

        when(_userRepo.findByToken(userToken)).thenReturn(user);
        when(_2faServices.isOptValid("SECRET", 999999)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> _userServices.verifyAndEnable2fa(userToken, request));
        assertFalse(user.getIsTwoFactorEnabled());
    }

    @Test
    @DisplayName("Verify and Enable 2FA: Throws Exception if secret not generated")
    void verifyAndEnable2fa_ShouldThrowException_WhenSecretMissing() {
        String userToken = "user-token";
        Users user = new Users();
        user.setMfaSecret(null);

        when(_userRepo.findByToken(userToken)).thenReturn(user);

        assertThrows(IllegalStateException.class, () -> _userServices
                .verifyAndEnable2fa(userToken, new Verify2faRequest()));
    }
}