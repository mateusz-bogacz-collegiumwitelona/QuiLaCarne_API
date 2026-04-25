package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.CreateBanRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IBanServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/ban", produces = "application/json")
@RequiredArgsConstructor
public class BanController {
    private final IBanServices _banServices;

    @Operation(
            summary = "Ban a user manually",
            description = "Allows a manager to manually ban a client without needing a prior report. " +
                    "The targeted user must have the ROLE_CLIENT. " +
                    "Automatically deactivates the user and sends an email notification. Requires MANAGER role.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ban created successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (e.g., missing fields) or the targeted user is not a client",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Valid JWT token is required",
                    content = @Content
            ),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - Requires ROLE_MANAGER role",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Targeted client or internal status not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error (e.g., email sending failure)",
                    content = @Content
            )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> create(
            @AuthenticationPrincipal(expression = "token") String adminToken,
            @Valid @RequestBody CreateBanRequest request
    ) {
        _banServices.create(adminToken, request);
        return ResponseEntity.ok(ResultHandler.success(
                "Ban created successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Get list of bans statuses",
            description = "Retrieves a dictionary list of all bans statuses available in the system. " +
                    "The names are translated based on the 'Accept-Language' header.",
            tags = {"Manager"}
    )
    @Parameter(
            name = "Accept-Language",
            in = ParameterIn.HEADER,
            description = "Preferred language (e.g., 'pl' or 'en')",
            schema = @Schema(type = "string", defaultValue = "pl", allowableValues = {"pl", "en"})
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dictionary review successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    @GetMapping("/dictionary")
    public ResponseEntity<ResultHandler<DictionaryResponse>> getDictionary() {
        var result = _banServices.getDictionary();
        return ResponseEntity.ok(
                ResultHandler.success(
                        "Dictionary review successfully",
                        HttpStatus.OK.value(),
                        result
                )
        );
    }
}
