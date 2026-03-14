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
import org.springframework.http.ResponseEntity;
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
    @PatchMapping("/password")
    public ResponseEntity<ResultHandler<String>> updatePassword(
            @RequestBody @Valid UpdatePasswordRequest request,
            @AuthenticationPrincipal(expression = "token") String userToken) {
        var result = _userServices.updatePassword(userToken, request);

        return ResponseEntity
                .status(result.getStatusCode())
                .body(result);
    }

    @PatchMapping("email/update")
    public ResponseEntity<ResultHandler<String>> updateEmail(
            @RequestParam
            @Parameter(description = "New email")
            @Email
            @NotBlank
            String email,
            @AuthenticationPrincipal(expression = "token") String userToken

    ) {
        var result = _userServices.updateEmail(userToken, email);
        return ResponseEntity
                .status(result.getStatusCode())
                .body(result);
    }

    @PatchMapping("/email/confirm")
    public ResponseEntity<ResultHandler<String>> confirmEmail(
            @RequestParam(name = "verificationToken")
            @NotBlank
            @Parameter(description = "New email confirm token")
            String verificationToken,
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        var result = _userServices.confirmEmailChange(userToken, verificationToken);

        return ResponseEntity
                .status(result.getStatusCode())
                .body(result);
    }
}
