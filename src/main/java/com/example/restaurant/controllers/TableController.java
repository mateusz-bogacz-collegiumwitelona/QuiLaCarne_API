package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.ITableServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/tables", produces = "application/json")
@RequiredArgsConstructor
public class TableController {
    private final ITableServices _tableServices;

    @Operation(
            summary = "Get full list of restaurant tables or check availability",
            description = "Returns a list of tables. If startTime and endTime are provided, it filters out tables that are reserved or unavailable in that timeframe. " +
                    "The names of table status are translated based on the 'Accept-Language' header."
    )
    @Parameter(
            name = "Accept-Language",
            in = ParameterIn.HEADER,
            description = "Preferred language (e.g., 'pl' or 'en')",
            required = false,
            schema = @Schema(type = "string", defaultValue = "pl", allowableValues = {"pl", "en"})
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Table list retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ResultHandler<List<TableListResponse>>> getTables(@Valid TableFilterRequest request) {
        var result = _tableServices.getTables(request);
        return ResponseEntity.status(result.getStatusCode()).body(result);
    }

    @Operation(
            summary = "Change table status to cleaning",
            description = "Updates the status of a specific table to CLEANING. This is typically used by waiters to indicate that a table needs to be prepared for the next guests."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Table status changed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_WAITER role"),
            @ApiResponse(responseCode = "404", description = "Table or table status not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_WAITER')")
    @PatchMapping("/{token}/clear")
    public ResponseEntity<ResultHandler<Void>> clearTables(@Parameter(description = "Table token") @PathVariable String token) {
        var result = _tableServices.changeStatusToClean(token);
        return ResponseEntity.status(result.getStatusCode()).body(result);
    }
}
