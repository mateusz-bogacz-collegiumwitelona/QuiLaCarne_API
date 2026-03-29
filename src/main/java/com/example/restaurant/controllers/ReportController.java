package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.ChangeReportStatusRequest;
import com.example.restaurant.dto.request.ReportFilterRequest;
import com.example.restaurant.dto.response.ReportListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IReportServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/report", produces = "application/json")
@RequiredArgsConstructor
public class ReportController {
    private final IReportServices _reportServices;

    @Operation(
            summary = "Create a new guest report",
            description = "Creates a new incident report for a specific client. The user being reported must have the ROLE_CLIENT. The report is created with an initial status of IN_PROGRESS. Requires WAITER or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Report created successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation failed or the reported user is not a client"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_WAITER or ROLE_MANAGER role"),
            @ApiResponse(responseCode = "404", description = "Reported client or internal status not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_WAITER', 'ROLE_MANAGER')")
    @PostMapping
    public ResponseEntity<ResultHandler<Void>> add(
            @Valid @RequestBody AddReportRequest request,
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        var result = _reportServices.add(userToken, request);
        return ResponseEntity.status(result.getStatusCode()).body(result);
    }


    @Operation(
            summary = "List guest reports",
            description = "Retrieves a paginated and filtered list of guest reports. Supports filtering by date range, status, and sorting by creation date. Requires MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reports retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "{\n  \"isSuccess\": true,\n  \"message\": \"Reports retrieved successfully\",\n  \"statusCode\": 200,\n  \"data\": {\n    \"items\": [\n      {\n        \"token\": \"550e8400-e29b-41d4-a716-446655440000\",\n        \"guestUsername\": \"john_doe\",\n        \"guestToken\": \"330e8400-e29b-41d4-a716-116655440000\",\n        \"reporterUsername\": \"waiter_anna\",\n        \"reporterToken\": \"110e8400-e29b-41d4-a716-226655440000\",\n        \"reason\": \"Guest was extremely rude and broke a glass.\",\n        \"createdAt\": \"2024-03-29T18:30:00Z\",\n        \"status\": \"In progress\"\n      }\n    ],\n    \"pageNumber\": 1,\n    \"pageSize\": 10,\n    \"totalPages\": 1,\n    \"totalElements\": 1,\n    \"isFirst\": true,\n    \"isLast\": true\n  }\n}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters (e.g., wrong date format)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<PagedResult<ReportListResponse>>> list(
            @ParameterObject @ModelAttribute @Valid ReportFilterRequest request
    ) {
        var result = _reportServices.list(request);
        return ResponseEntity.status(result.getStatusCode()).body(result);
    }

    @Operation(
            summary = "Change report status and process bans",
            description = "Allows a manager to accept or reject an existing guest report. If the report is accepted (`isAccepted: true`), the system automatically issues a ban for the reported guest until the specified `expiresAt` date and sends an email notification. Requires MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Report status changed successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., missing token or invalid date format)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Report or required internal statuses not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error (e.g., email sending failure)", content = @Content)
    })
    @PutMapping("/change-status")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> changeStatus(
            @AuthenticationPrincipal(expression = "token") String adminToken,
            @RequestBody @Valid ChangeReportStatusRequest request
    ) {
        var result = _reportServices.changeStatus(adminToken, request);
        return ResponseEntity.status(result.getStatusCode()).body(result);
    }

}
