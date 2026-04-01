package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.*;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IUserServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/user", produces = "application/json")
@RequiredArgsConstructor
public class UserController {
    private final IUserServices _userServices;

    @Operation(
            summary = "Update user password",
            description = "Allows an authenticated user to change their password by verifying the current password."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid old password or validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_CLIENT')")
    @PatchMapping("/me/password")
    public ResponseEntity<ResultHandler<Void>> updatePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal(expression = "token") String userToken) {
        _userServices.updatePassword(userToken, request);

        return ResponseEntity.ok(ResultHandler.success(
                "Password updated",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Request email update",
            description = "Initiates the email update process by saving the new email in a pending state and sending a verification link to it."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification link sent to the new email address"),
            @ApiResponse(responseCode = "400", description = "Email is invalid or already in use by another user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_CLIENT')")
    @PatchMapping("/me/email/update")
    public ResponseEntity<ResultHandler<Void>> updateEmail(
            @RequestParam
            @Parameter(description = "New email")
            @Email
            @NotBlank
            String email,
            @AuthenticationPrincipal(expression = "token") String userToken

    ) {
        _userServices.updateEmail(userToken, email);
        return ResponseEntity.ok(ResultHandler.success(
                "Verification link sent to the new email",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Confirm email change",
            description = "Finalizes the email update process by validating the verification token sent to the user's new email address."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid/expired token, or no pending email update found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_CLIENT')")
    @PatchMapping("/me/email/confirm")
    public ResponseEntity<ResultHandler<Void>> confirmEmail(
            @RequestParam(name = "verificationToken")
            @NotBlank
            @Parameter(description = "New email confirm token")
            String verificationToken,
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        _userServices.confirmEmailChange(userToken, verificationToken);
        return ResponseEntity.ok(ResultHandler.success(
                "Email updated successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(summary = "Update username", description = "Allows the authenticated user to change their display name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Username updated successfully"),
            @ApiResponse(responseCode = "400", description = "Username already taken or invalid"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('ROLE_CLIENT')")
    @PatchMapping("/me/username")
    public ResponseEntity<ResultHandler<Void>> updateUserName(
            @RequestParam
            @NotBlank
            @Parameter(description = "New user name")
            String userName,
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        _userServices.updateUserName(userName, userToken);
        return ResponseEntity.ok(ResultHandler.success(
                "User name changed successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(summary = "Delete user", description = "Allows the authenticated user to delete account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Can't delte user"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('ROLE_CLIENT')")
    @DeleteMapping("/me/delete")
    public ResponseEntity<ResultHandler<Void>> deleteUser(@AuthenticationPrincipal(expression = "token") String userToken) {
        _userServices.delete(userToken);

        return ResponseEntity.ok(ResultHandler.success(
                "User deleted successfully",
                HttpStatus.OK.value())
        );
    }

    @Operation(
            summary = "Add a new employee",
            description = "Creates a new employee account (Manager or Waiter) and automatically activates it. Requires ROLE_MANAGER privileges."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Employee created successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data or email/username already exists", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have the required ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/employee")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> createEmployee(
            @Valid @RequestBody AddEmployeeRequest request
    ) {
        _userServices.createEmployee(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResultHandler.success(
                        "Employee created successfully",
                        HttpStatus.CREATED.value()
                )
        );
    }

    @Operation(
            summary = "Edit employee details",
            description = "Updates the email and/or username of an existing employee. Only provided fields will be updated. Requires ROLE_MANAGER privileges."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee updated successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data, email/username already taken, or values are the same as current", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have the required ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Employee token does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PatchMapping("/employee")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> editEmployee(
            @Valid @RequestBody EditEmployeeRequest request
    ) {
        _userServices.editEmployee(request);

        return ResponseEntity.ok(
                ResultHandler.success(
                        "Employee updated successfully",
                        HttpStatus.OK.value()
                )
        );
    }

    @Operation(
            summary = "Change employee password",
            description = "Allows a manager to forcefully change an employee's password. Managers cannot change their own password using this endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully", content = @Content(schema = @Schema(implementation = ResultHandler.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Passwords do not match or invalid format", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ROLE_MANAGER role or tried to change own password", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Employee token does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PatchMapping("/employee/change-password")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> changeEmployeePassword(
            @AuthenticationPrincipal(expression = "token") String adminToken,
            @Valid @RequestBody ChangeEmployeePasswordRequest request
    ) {
        _userServices.changeEmployeePassword(adminToken, request);

        return ResponseEntity.ok(ResultHandler.success(
                "Employee password changed successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Change employee role",
            description = "Allows a manager to change the role of an employee (e.g., promote to Manager or demote to Waiter). Managers cannot change their own role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee role updated successfully", content = @Content(schema = @Schema(implementation = ResultHandler.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ROLE_MANAGER role or tried to change own role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Employee token does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PatchMapping("/employee/change-role")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> changeEmployeeRole(
            @AuthenticationPrincipal(expression = "token") String adminToken,
            @Valid @RequestBody ChangeEmployeeRoleRequest request
    ) {
        _userServices.changeEmployeeRole(adminToken, request);

        return ResponseEntity.ok(ResultHandler.success(
                "Employee role changed successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Change employee availability (Block/Unblock)",
            description = "Allows a manager to block or unblock an employee's access to the system. Managers cannot change their own availability."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee availability changed successfully", content = @Content(schema = @Schema(implementation = ResultHandler.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Status is already the same or invalid input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ROLE_MANAGER role or tried to block themselves", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Employee token does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PatchMapping("/employee/change-availability")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> blockEmployee(
            @AuthenticationPrincipal(expression = "token") String adminToken,
            @Valid @RequestBody BlockEmployeeRequest request
    ) {
        _userServices.blockEmployee(adminToken, request);

        return ResponseEntity.ok(ResultHandler.success(
                "Employee availability changed successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Delete employee",
            description = "Performs a soft delete of an employee account, anonymizing their personal data and deactivating the account. Managers cannot delete themselves."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee deleted successfully", content = @Content(schema = @Schema(implementation = ResultHandler.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ROLE_MANAGER role or tried to delete themselves", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Employee token does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping("/employee/{employeeToken}/delete")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> deleteEmployee(
            @AuthenticationPrincipal(expression = "token") String adminToken,
            @PathVariable("employeeToken") @Parameter(description = "Token of the employee to delete") String employeeToken
    ) {
        _userServices.deleteEmployee(adminToken, employeeToken);

        return ResponseEntity.ok(ResultHandler.success(
                "Employee deleted successfully",
                HttpStatus.OK.value()
        ));
    }
}
