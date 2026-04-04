package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.AddTableRequest;
import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.EntityResponse;
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
import org.springframework.http.HttpStatus;
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
        return ResponseEntity.ok(
                ResultHandler.success("Tables reviewed sucessfully",
                        HttpStatus.OK.value(),
                        result
                ));
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
        _tableServices.changeStatusToClean(token);
        return ResponseEntity.ok(ResultHandler.success(
                "Status changed successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Change table status to out of service",
            description = "Updates the status of a specific table to OUT_OF_SERVICE. This is typically used by waiters to indicate that a table have a problem (leg is broken)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Table status changed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_WAITER role"),
            @ApiResponse(responseCode = "404", description = "Table or table status not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_WAITER')")
    @PatchMapping("/{token}/out-of-services")
    public ResponseEntity<ResultHandler<Void>> changeStatusToOutOfServices(@Parameter(description = "Table token") @PathVariable String token) {
        _tableServices.changeStatusToOutOfService(token);
        return ResponseEntity.ok(ResultHandler.success(
                "Status changed successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Add a new restaurant table",
            description = "Creates a new table with the specified number and capacity. The table is automatically assigned the default 'AVAILABLE' status. Requires ROLE_MANAGER."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Table created successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request - Table number already exists or invalid input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> addTable(
            @Valid @RequestBody AddTableRequest request
    ) {
        _tableServices.add(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResultHandler.success(
                        "Table added successfully",
                        HttpStatus.CREATED.value()
                )
        );
    }

    @Operation(
            summary = "Delete a restaurant table",
            description = "Performs a soft delete of a table. It will no longer be visible in the active tables list. Requires ROLE_MANAGER."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Table deleted successfully", content = @Content(schema = @Schema(implementation = ResultHandler.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping("/{token}/delete")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> deleteTable(
            @Parameter(description = "Table token") @PathVariable String token
    ) {
        _tableServices.delete(token);

        return ResponseEntity.ok(ResultHandler.success(
                "Table deleted successfully",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Get list of table statuses",
            description = "Retrieves a dictionary list of all table statuses available in the system. The names are translated based on the 'Accept-Language' header."
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
                    description = "Dictionary review successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/dictionary")
    public ResponseEntity<ResultHandler<List<EntityResponse>>> getDictionary() {
        var result = _tableServices.getDictionary();
        return ResponseEntity.ok(
                ResultHandler.success(
                        "Dictionary review successfully",
                        HttpStatus.OK.value(),
                        result
                )
        );
    }

    @Operation(
            summary = "Add a new table status",
            description = "Creates a new table status in the system. The English name is automatically used to generate a unique token. Requires MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Table status created successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error or table status already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    @PostMapping("/status/add")
    public ResponseEntity<ResultHandler<Void>> addStatus(@RequestBody @Valid AddEntityRequest request) {
        _tableServices.addStatus(request);
        return ResponseEntity.ok(ResultHandler.success(
                "Table status created successfully",
                HttpStatus.CREATED.value()
        ));
    }
}
