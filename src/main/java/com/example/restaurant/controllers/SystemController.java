package com.example.restaurant.controllers;

import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.ISystemServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/system", produces = "application/json")
@RequiredArgsConstructor
public class SystemController {
  private final ISystemServices _systemServices;

  @Operation(
      summary = "Clear all caches",
      description = "Flushes all Redis caches managed by the application. Requires ROLE_MANAGER.",
      tags = {"Manager"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "All caches cleared successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires manager privileges")
      })
  @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
  @DeleteMapping("/cache/clear-all")
  public ResponseEntity<ResultHandler<Void>> clearAllCaches() {
    _systemServices.clearAllCache();

    return ResponseEntity.ok(
        ResultHandler.success("All caches have been successfully cleared", HttpStatus.OK.value()));
  }

  @Operation(
      summary = "Clear specific cache",
      description =
          "Flushes a specific Redis cache by its name (e.g., 'usersList', 'dishMenu'). "
              + "Requires ROLE_MANAGER.",
      tags = {"Manager"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Specific cache cleared successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Cache with the given name does not exist"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires manager privileges")
      })
  @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
  @DeleteMapping("/cache/clear/{cacheName}")
  public ResponseEntity<ResultHandler<Void>> clearSpecificCache(
      @PathVariable @Parameter(description = "Name of the cache to clear") String cacheName) {
    _systemServices.clearSpecificCache(cacheName);

    return ResponseEntity.ok(
        ResultHandler.success(
            "Cache '" + cacheName + "' cleared successfully", HttpStatus.OK.value()));
  }

  @Operation(
      summary = "Review cache List",
      description =
          "Review list of all Redis caches names (e.g., 'usersList', 'dishMenu'). "
              + "Requires ROLE_MANAGER.",
      tags = {"Manager"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "List review successully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires manager privileges")
      })
  @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
  @GetMapping("/cache/list")
  public ResponseEntity<ResultHandler<List<String>>> getCacheList() {
    var result = _systemServices.getCacheList();

    return ResponseEntity.ok(
        ResultHandler.success("List review successully", HttpStatus.OK.value(), result));
  }
}
