package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.AddReportRequest;
import com.example.restaurant.dto.request.ChangeReportStatusRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IReportServices;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping(value = "/api/report", produces = "application/json")
@RequiredArgsConstructor
public class ReportController {
  private final IReportServices _reportServices;

  @Operation(
      summary = "Create a new guest report",
      description =
          "Creates a new incident report for a specific client. "
              + "The user being reported must have the ROLE_CLIENT. "
              + "The report is created with an initial status of IN_PROGRESS. Requires WAITER or MANAGER role.",
      tags = {"Manager", "Waiter"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Report created successfully",
            content = @Content(schema = @Schema(implementation = ResultHandler.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed or the reported user is not a client"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Valid JWT token is required"),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Requires ROLE_WAITER or ROLE_MANAGER role"),
        @ApiResponse(
            responseCode = "404",
            description = "Reported client or internal status not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PreAuthorize("hasAnyRole('ROLE_WAITER', 'ROLE_MANAGER')")
  @PostMapping
  public ResponseEntity<ResultHandler<Void>> add(
      @Valid @RequestBody AddReportRequest request,
      @AuthenticationPrincipal(expression = "token") String userToken) {
    _reportServices.add(userToken, request);
    return ResponseEntity.ok(
        ResultHandler.success("Report created successfully", HttpStatus.CREATED.value()));
  }

  @Operation(
      summary = "Change report status and process bans",
      description =
          "Allows a manager to accept or reject an existing guest report. "
              + "If the report is accepted (`isAccepted: true`), "
              + "the system automatically issues a ban for the reported guest "
              + "until the specified `expiresAt` date and sends an email notification. "
              + "Requires MANAGER role.",
      tags = {"Manager"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Report status changed successfully",
            content = @Content(schema = @Schema(implementation = ResultHandler.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error (e.g., missing token or invalid date format)",
            content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Valid JWT token is required",
            content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Requires ROLE_MANAGER role",
            content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Report or required internal statuses not found",
            content = @Content),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error (e.g., email sending failure)",
            content = @Content)
      })
  @PutMapping("/change-status")
  @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
  public ResponseEntity<ResultHandler<Void>> changeStatus(
      @AuthenticationPrincipal(expression = "token") String adminToken,
      @RequestBody @Valid ChangeReportStatusRequest request) {
    _reportServices.changeStatus(adminToken, request);
    return ResponseEntity.ok(
        ResultHandler.success("Report status changed successfuly", HttpStatus.OK.value()));
  }
}
