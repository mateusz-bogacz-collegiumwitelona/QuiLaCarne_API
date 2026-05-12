package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.fasade.interfaces.IReservationFacade;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/reservations", produces = "application/json")
@RequiredArgsConstructor
public class ReservationController {
  private final IReservationFacade _reservationServices;

  @Operation(
      summary = "Create a new table reservation",
      description =
          "Creates a new table reservation for the authenticated user. "
              + "Optionally, a list of dishes can be provided to pre-order food for the reservation.",
      tags = {"Client"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Reservation created successfully",
            content = @Content(schema = @Schema(implementation = ResultHandler.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or dates"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Valid JWT token is required"),
        @ApiResponse(responseCode = "404", description = "Table or dish not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Table is already reserved in the specified timeframe"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PreAuthorize("hasAnyRole('ROLE_CLIENT')")
  @PostMapping
  public ResponseEntity<ResultHandler<ReservationResponse>> create(
      @RequestBody @Valid ReservationRequest request,
      @AuthenticationPrincipal(expression = "token") String userToken) {
    var result = _reservationServices.create(request, userToken);

    return ResponseEntity.ok(
        ResultHandler.success(
            "Reservation created successfully", HttpStatus.CREATED.value(), result));
  }

  @Operation(
      summary = "Get user reservations history",
      description =
          "Retrieves a paginated and filterable list of reservations "
              + "made by the currently authenticated user. "
              + "Supports filtering by date range and status.",
      tags = {"Manager", "Client", "Waiter"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Reservations retrieved successfully",
            content = @Content(schema = @Schema(implementation = ResultHandler.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Valid JWT token is required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping
  public ResponseEntity<ResultHandler<PagedResult<ClientReservationResponse>>> list(
      @ParameterObject @Valid @ModelAttribute PaggedRequest pagged,
      @ParameterObject @Valid @ModelAttribute ClientReservationRequest request,
      @AuthenticationPrincipal(expression = "token") String userToken) {
    var result = _reservationServices.history(request, pagged, userToken);

    return ResponseEntity.ok(
        ResultHandler.success(
            "User reservations retrieved successfully", HttpStatus.OK.value(), result));
  }

  @Operation(
      summary = "Get reservation details",
      description =
          "Retrieves the full details of a specific reservation, "
              + "including pre-ordered dishes and total price.",
      tags = {"Manager", "Client", "Waiter"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Reservation details retrieved successfully",
            content = @Content(schema = @Schema(implementation = ResultHandler.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Valid JWT token is required"),
        @ApiResponse(responseCode = "404", description = "Reservation not found or access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/{token}")
  public ResponseEntity<ResultHandler<ReservationDetailsResponse>> detail(
      @Parameter(description = "Reservaton token ") @PathVariable String token,
      @AuthenticationPrincipal(expression = "token") String userToken) {

    var result = _reservationServices.details(token, userToken);
    return ResponseEntity.ok(
        ResultHandler.success(
            "User reservations details retrieved successfully", HttpStatus.OK.value(), result));
  }

  @Operation(
      summary = "Cancel a reservation",
      description = "Cancels an active reservation by changing its status to CANCELLED.",
      tags = {"Client"})
  @PreAuthorize("hasAnyRole('ROLE_CLIENT')")
  @PatchMapping("/{token}/cancel")
  public ResponseEntity<ResultHandler<Void>> cancel(
      @Parameter(description = "Reservation token") @PathVariable String token,
      @AuthenticationPrincipal(expression = "token") String userToken) {
    _reservationServices.cancel(token, userToken);
    return ResponseEntity.ok(
        ResultHandler.success("Reservation cancelled successfully", HttpStatus.OK.value()));
  }

  @Operation(
      summary = "Remove a dish from reservation",
      description =
          "Decreases the quantity of a specific dish in the reservation's order. "
              + "If the quantity to remove is equal to or greater than the current quantity, "
              + "the dish is completely removed from the order.",
      tags = {"Waiter"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dish removed successfully",
            content = @Content(schema = @Schema(implementation = ResultHandler.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Valid JWT token is required"),
        @ApiResponse(responseCode = "404", description = "Reservation or dish not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PreAuthorize("hasAnyRole('ROLE_WAITER')")
  @DeleteMapping("/item/remove")
  public ResponseEntity<ResultHandler<Void>> removeItem(
      @AuthenticationPrincipal(expression = "token") String waiterToken,
      @RequestParam String reservationToken,
      @Valid @RequestBody ReservationDishRequest request) {
    _reservationServices.removeItemFromReservation(waiterToken, reservationToken, request);
    return ResponseEntity.ok(
        ResultHandler.success("Order item removed successfully", HttpStatus.OK.value()));
  }

  @Operation(
      summary = "Add multiple dishes to a reservation",
      description =
          "Allows adding as many new dishes as you want to an existing reservation in a single request. "
              + "Pass a list of dishes in the request body. "
              + "If a requested dish (with the exact same note) already exists on the order, "
              + "its quantity will be automatically increased. "
              + "If it is a new dish or has a different note, it will be added as a separate line item. "
              + "The total price of the order is automatically recalculated.",
      tags = {"Waiter"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Items added successfully",
            content = @Content(schema = @Schema(implementation = ResultHandler.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data (e.g. missing dish tokens)"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Valid JWT token is required"),
        @ApiResponse(
            responseCode = "404",
            description = "Reservation or dish not found / access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PreAuthorize("hasAnyRole('ROLE_WAITER')")
  @PostMapping("/item/add")
  public ResponseEntity<ResultHandler<Void>> addItem(
      @AuthenticationPrincipal(expression = "token") String waiterToken,
      @RequestParam String reservationToken,
      @Valid @RequestBody List<ReservationDishRequest> request) {
    _reservationServices.addItemFromReservation(waiterToken, reservationToken, request);
    return ResponseEntity.ok(
        ResultHandler.success("Order items add successfully", HttpStatus.OK.value()));
  }

  @Operation(
      summary = "Assign waiter to a reservation",
      description =
          "Assigns a specific waiter to a reservation and its associated order. "
              + "Automatically changes the status of the order and pending dishes to IN_PROGRESS.",
      tags = {"Waiter"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Waiter assigned successfully",
            content = @Content(schema = @Schema(implementation = ResultHandler.class))),
        @ApiResponse(responseCode = "403", description = "User does not have waiter privileges"),
        @ApiResponse(responseCode = "404", description = "Reservation or Waiter not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PreAuthorize("hasAnyRole('ROLE_WAITER')")
  @PatchMapping("/{token}/assign-waiter")
  public ResponseEntity<ResultHandler<Void>> assignWaiter(
      @Parameter(description = "Reservation token") @PathVariable String token,
      @AuthenticationPrincipal(expression = "token") String waiterToken) {
    _reservationServices.assignWaiter(token, waiterToken);
    return ResponseEntity.ok(
        ResultHandler.success("Waiters assigned successfully", HttpStatus.OK.value()));
  }

  @Operation(
      summary = "Mark reservation as absent (No-show)",
      description =
          "Changes the reservation status to NO_SHOW. "
              + "If an order is attached to the reservation, "
              + "its status (and the status of its items) is changed to CANCELLED. "
              + "Requires the reservation to be in the ACTIVE state. Available only for waiters.",
      tags = {"Waiter"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully marked the reservation and orders as absent"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid state (e.g., reservation is not in the ACTIVE state)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires ROLE_WAITER role"),
        @ApiResponse(
            responseCode = "404",
            description = "Reservation with the provided token not found"),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during status change")
      })
  @PreAuthorize("hasAnyRole('ROLE_WAITER')")
  @PatchMapping("/{token}/absent")
  public ResponseEntity<ResultHandler<Void>> absent(
      @Parameter(description = "Reservation token") @PathVariable String token) {
    _reservationServices.isAbsent(token);
    return ResponseEntity.ok(ResultHandler.success("Absent success", HttpStatus.OK.value()));
  }

  @Operation(
      summary = "Get list of reservation statuses",
      description =
          "Retrieves a dictionary list of all reservation statuses available in the system. "
              + "The names are translated based on the 'Accept-Language' header.",
      tags = {"Manager", "Client", "Waiter"})
  @Parameter(
      name = "Accept-Language",
      in = ParameterIn.HEADER,
      description = "Preferred language (e.g., 'pl' or 'en')",
      schema =
          @Schema(
              type = "string",
              defaultValue = "pl",
              allowableValues = {"pl", "en"}))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dictionary review successfully",
            content = @Content(schema = @Schema(implementation = ResultHandler.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content)
      })
  @GetMapping("/dictionary")
  public ResponseEntity<ResultHandler<DictionaryResponse>> getDictionary() {
    var result = _reservationServices.getDictionary();
    return ResponseEntity.ok(
        ResultHandler.success("Dictionary review successfully", HttpStatus.OK.value(), result));
  }
}
