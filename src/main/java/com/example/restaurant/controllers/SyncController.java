package com.example.restaurant.controllers;

import com.example.restaurant.dto.response.*;
import com.example.restaurant.helpers.PagedResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/sync", produces = "application/json")
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
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires appropriate role"),
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

    @Operation(
            summary = "Fetch all system dictionaries",
            description = "Returns flat lists of all dictionaries " +
                    "(statuses, categories, allergens) with EN/PL translations, perfect for saving in local SQLite"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dictionaries fetched successfully",
                    content = @Content(schema = @Schema(implementation = SyncDictionariesResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "No authorization"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires appropriate role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/dictionaries")
    @PreAuthorize("hasAnyRole('ROLE_WAITER', 'ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<SyncDictionariesResponse>> getDictionaries() {
        var result = _syncServices.getDictionaries();
        return ResponseEntity.ok(ResultHandler.success(
                "Dictionaries fetched successfully",
                HttpStatus.OK.value(),
                result
        ));
    }

    @Operation(
            summary = "Fetch system roles",
            description = "Returns a flat list of all available roles in the system. " +
                    "Used to synchronize the roles dictionary on client devices."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles fetched successfully"),
            @ApiResponse(responseCode = "401", description = "No authorization"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires appropriate role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ROLE_WAITER', 'ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<List<SyncRoleResponse>>> getRoles() {
        var result = _syncServices.getRoles();
        return ResponseEntity.ok(ResultHandler.success(
                "Roles fetched successfully",
                HttpStatus.OK.value(),
                result
        ));
    }

    @Operation(
            summary = "Fetch flat list of dishes",
            description = "Returns a paginated, flat list of dishes with foreign key tokens. " +
                    "Page size is strictly fixed by the server."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dishes sync page fetched successfully"),
            @ApiResponse(responseCode = "401", description = "No authorization"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires appropriate role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/dishes")
    @PreAuthorize("hasAnyRole('ROLE_WAITER', 'ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<PagedResult<SyncDishResponse>>> getDishesSync(
            @RequestParam(defaultValue = "1") int page
    ) {
        var result = _syncServices.getDishesSync(page);

        return ResponseEntity.ok(ResultHandler.success(
                "Dishes sync page fetched successfully",
                HttpStatus.OK.value(),
                result
        ));
    }

    @Operation(
            summary = "Fetch flat list of bans",
            description = "Returns a paginated, flat list of bans with foreign key tokens (user, bannedBy, statuses). " +
                    "Page size is strictly fixed by the server."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bans sync page fetched successfully"),
            @ApiResponse(responseCode = "401", description = "No authorization"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires appropriate role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/bans")
    @PreAuthorize("hasAnyRole('ROLE_WAITER', 'ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<PagedResult<SyncBanResponse>>> getBansSync(
            @RequestParam(defaultValue = "1") int page
    ) {
        var result = _syncServices.getBansSync(page);

        return ResponseEntity.ok(ResultHandler.success(
                "Bans sync page fetched successfully",
                HttpStatus.OK.value(),
                result
        ));
    }

    @Operation(
            summary = "Fetch flat list of reports",
            description = "Returns a paginated, " +
                    "flat list of guest reports with foreign key tokens (guest, reporter, statuses). " +
                    "Page size is strictly fixed by the server."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reports sync page fetched successfully"),
            @ApiResponse(responseCode = "401", description = "No authorization"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires appropriate role")
    })
    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ROLE_WAITER', 'ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<PagedResult<SyncReportResponse>>> getReportsSync(
            @RequestParam(defaultValue = "1") int page
    ) {
        var result = _syncServices.getReportsSync(page);

        return ResponseEntity.ok(ResultHandler.success(
                "Reports sync page fetched successfully",
                HttpStatus.OK.value(),
                result
        ));
    }

    @Operation(
            summary = "Fetch flat list of ingredients (Sync)",
            description = "Returns a paginated, flat list of ingredients with their " +
                    "translations and associated allergen tokens."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingredients sync page fetched successfully"),
            @ApiResponse(responseCode = "401", description = "No authorization"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires appropriate role")
    })
    @GetMapping("/ingredients")
    @PreAuthorize("hasAnyRole('ROLE_WAITER', 'ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<PagedResult<SyncIngredientResponse>>> getIngredientsSync(
            @RequestParam(defaultValue = "1") int page
    ) {
        var result = _syncServices.getIngredientsSync(page);

        return ResponseEntity.ok(ResultHandler.success(
                "Ingredients sync page fetched successfully",
                HttpStatus.OK.value(),
                result
        ));
    }
}
