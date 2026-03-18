package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.dto.response.TodayReservationsResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IReservationServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/reservations", produces = "application/json")
@RequiredArgsConstructor
public class ReservationController {
    private final IReservationServices _reservationServices;

    @Operation(
            summary = "Create a new table reservation",
            description = "Creates a new table reservation for the authenticated user. " +
                    "Optionally, a list of dishes can be provided to pre-order food for the reservation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Reservation created successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data or dates"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "404", description = "Table or dish not found"),
            @ApiResponse(responseCode = "409", description = "Table is already reserved in the specified timeframe"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ResultHandler<ReservationResponse>> create(
            @RequestBody @Valid ReservationRequest request,
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        var result = _reservationServices.create(request, userToken);

        return ResponseEntity
                .status(result.getStatusCode())
                .body(result);
    }

    @Operation(
            summary = "Get user reservations history",
            description = "Retrieves a paginated and filterable list of reservations made by the currently authenticated user. " +
                    "Supports filtering by date range and status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reservations retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ResultHandler<PagedResult<ClientReservationResponse>>> list(
            @ParameterObject @Valid @ModelAttribute PaggedRequest pagged,
            @ParameterObject @Valid @ModelAttribute ClientReservationRequest request,
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        var result = _reservationServices.history(request, pagged, userToken);

        return ResponseEntity
                .status(result.getStatusCode())
                .body(result);
    }

    @Operation(
            summary = "Get reservation details",
            description = "Retrieves the full details of a specific reservation, including pre-ordered dishes and total price."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reservation details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "404", description = "Reservation not found or access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{token}")
    public ResponseEntity<ResultHandler<ReservationDetailsResponse>> detail(
            @Parameter(description = "Reservaton token ")
            @PathVariable
            String token,
            @AuthenticationPrincipal(expression = "token") String userToken

    ) {
        var result = _reservationServices.details(token, userToken);
        return ResponseEntity
                .status(result.getStatusCode())
                .body(result);
    }

    @Operation(
            summary = "Cancel a reservation",
            description = "Cancels an active reservation by changing its status to CANCELLED."
    )
    @PatchMapping("/{token}/cancel")
    public ResponseEntity<ResultHandler<Boolean>> cancel(
            @Parameter(description = "Reservation token")
            @PathVariable String token,
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        var result = _reservationServices.cancel(token, userToken);
        return ResponseEntity
                .status(result.getStatusCode())
                .body(result);
    }

    @Operation(
            summary = "Get today's reservations (Waiter/Manager)",
            description = "Retrieves a paginated list of all reservations scheduled for today. Includes basic table and user details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Today's reservations retrieved successfully", content = @Content(schema = @Schema(implementation = ResultHandler.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/today")
    public ResponseEntity<ResultHandler<PagedResult<TodayReservationsResponse>>> getTodayReservations(
            @ParameterObject @Valid @ModelAttribute PaggedRequest pagged
    ) {
        var result = _reservationServices.today(pagged);
        return ResponseEntity
                .status(result.getStatusCode())
                .body(result);
    }
}
