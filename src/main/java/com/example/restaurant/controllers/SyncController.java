package com.example.restaurant.controllers;

import com.example.restaurant.dto.response.SyncBootstrapResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.ISyncServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {
    private final ISyncServices _syncServices;

    @Operation(
            summary = "Download the sync manifest (Bootstrap)",
            description = "Returns the number of records and pages for each system module. " +
                    "Used to initialize the database on mobile and desktop devices."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Manifest downloaded successfully",
                    content = @Content(schema = @Schema(implementation = SyncBootstrapResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "No authorization"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/bootstrap")
    @PreAuthorize("hasAnyRole('ROLE_WAITER', 'ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<SyncBootstrapResponse>> getBootstrap() {
        var result = _syncServices.getBootstrapManifest();
        return ResponseEntity.ok(ResultHandler.success(
                "Manifest data get successfully",
                HttpStatus.OK.value(),
                result
        ));
    }
}
