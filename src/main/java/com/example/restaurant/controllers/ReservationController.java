package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IReservationServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestBody ReservationRequest request,
            @AuthenticationPrincipal(expression = "token") String userToken
    ) {
        var result = _reservationServices.create(request, userToken);

        return ResponseEntity.status(result.getStatusCode()).body(result);
    }
}
