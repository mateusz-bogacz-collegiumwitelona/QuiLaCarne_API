package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IUserServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping(value = "/api/user/me", produces = "application/json")
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
    @PatchMapping("/password")
    public ResponseEntity<ResultHandler<Void>> updatePassword(
            @RequestBody @Valid UpdatePasswordRequest request,
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
    @PatchMapping("email/update")
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
    @PatchMapping("/email/confirm")
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
    @PatchMapping("/username")
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
    @DeleteMapping("/delete")
    public ResponseEntity<ResultHandler<Void>> deleteUser(@AuthenticationPrincipal(expression = "token") String userToken) {
        _userServices.deleteAccount(userToken);

        return ResponseEntity.ok(ResultHandler.success(
                "User deleted successfully",
                HttpStatus.OK.value())
        );
    }
}
