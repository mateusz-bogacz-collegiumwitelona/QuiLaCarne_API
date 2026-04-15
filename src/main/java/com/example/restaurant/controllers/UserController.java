package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.Generate2faResponse;
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
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class UserController {
    private final IUserServices _userServices;

    @Operation(
            summary = "Update user password",
            description = "Allows an authenticated user to change their password by verifying the current password.",
            tags = {"Client"}
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
            description = "Initiates the email update process by saving the new email " +
                    "in a pending state and sending a verification link to it.",
            tags = {"Client"}
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
            description = "Finalizes the email update process by validating " +
                    "the verification token sent to the user's new email address.",
            tags = {"Client"}
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

    @Operation(
            summary = "Update username",
            description = "Allows the authenticated user to change their display name.",
            tags = {"Client"}
    )
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

    @Operation(
            summary = "Delete user",
            description = "Allows the authenticated user to delete account.",
            tags = {"Client"}
    )
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
            description = "Creates a new employee account (Manager or Waiter) and automatically activates it. " +
                    "Requires ROLE_MANAGER privileges.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Employee created successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid input data or email/username already exists",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User is not logged in",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have the required ROLE_MANAGER role",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
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
            description = "Updates the email and/or username of an existing employee. " +
                    "Only provided fields will be updated. Requires ROLE_MANAGER privileges.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee updated successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid input data, email/username already taken, " +
                            "or values are the same as current",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User is not logged in",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have the required ROLE_MANAGER role",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Employee token does not exist",
                    content = @Content
            ),
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
            description = "Allows a manager to forcefully change an employee's password. " +
                    "Managers cannot change their own password using this endpoint.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password updated successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Passwords do not match or invalid format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User is not logged in",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have ROLE_MANAGER role or tried to change own password",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Employee token does not exist",
                    content = @Content
            ),
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
            description = "Allows a manager to change the role of an employee " +
                    "(e.g., promote to Manager or demote to Waiter). Managers cannot change their own role.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee role updated successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have ROLE_MANAGER role or tried to change own role",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Employee token does not exist",
                    content = @Content
            ),
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
            description = "Allows a manager to block or unblock an employee's access to the system. " +
                    "Managers cannot change their own availability.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee availability changed successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Status is already the same or invalid input",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User is not logged in",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have ROLE_MANAGER role or tried to block themselves",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Employee token does not exist",
                    content = @Content
            ),
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
            description = "Performs a soft delete of an employee account, " +
                    "anonymizing their personal data and deactivating the account. Managers cannot delete themselves.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee deleted successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User is not logged in",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have ROLE_MANAGER role or tried to delete themselves",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Employee token does not exist",
                    content = @Content
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping("/employee/{employeeToken}/delete")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> deleteEmployee(
            @AuthenticationPrincipal(expression = "token")
            String adminToken,

            @PathVariable @Parameter(description = "Token of the employee to delete")
            String employeeToken
    ) {
        _userServices.deleteEmployee(adminToken, employeeToken);

        return ResponseEntity.ok(ResultHandler.success(
                "Employee deleted successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Generate 2FA QR code for setup",
            description = "Generates a new 2FA secret and returns a QR code URI along with a manual code. " +
                    "Does NOT enable 2FA yet. Requires ROLE_MANAGER.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "2FA secret generated successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "2FA is already enabled for this user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ROLE_MANAGER role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/2fa/generate")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Generate2faResponse>> generate2fa(
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        var response = _userServices.generate2fa(userToken);
        return ResponseEntity.ok(ResultHandler.success(
                "2FA secret generated successfully",
                HttpStatus.OK.value(),
                response
        ));
    }

    @Operation(
            summary = "Verify code and enable 2FA on account",
            description = "Verifies the 6-digit code from the authenticator app. " +
                    "If correct, permanently enables 2FA for the manager's account. Requires ROLE_MANAGER.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "2FA enabled successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid 2FA code or secret not generated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ROLE_MANAGER role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/2fa/enable")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> enable2fa(
            @AuthenticationPrincipal(expression = "token") String userToken,
            @Valid @RequestBody Verify2faRequest request
    ) {
        _userServices.verifyAndEnable2fa(userToken, request);
        return ResponseEntity.ok(ResultHandler.success(
                "2FA enabled successfully",
                HttpStatus.OK.value()
        ));
    }
}
