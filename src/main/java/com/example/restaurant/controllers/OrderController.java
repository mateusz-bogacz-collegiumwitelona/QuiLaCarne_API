package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IOrderServices;
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
@RequestMapping(value = "/api/order", produces = "application/json")
@RequiredArgsConstructor
public class OrderController {
    private final IOrderServices _orderServices;

    @Operation(
            summary = "Get list of order statuses",
            description = "Retrieves a dictionary list of all order statuses available in the system. The names are translated based on the 'Accept-Language' header."
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
        var result = _orderServices.getDictionary();
        return ResponseEntity.ok(
                ResultHandler.success(
                        "Dictionary review successfully",
                        HttpStatus.OK.value(),
                        result
                )
        );
    }

    @Operation(
            summary = "Get list of order item statuses",
            description = "Retrieves a dictionary list of all order item statuses available in the system. The names are translated based on the 'Accept-Language' header."
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
    @GetMapping("/item/dictionary")
    public ResponseEntity<ResultHandler<List<EntityResponse>>> getItemStatusesDictionary() {
        var result = _orderServices.getItemStatusesDictionary();
        return ResponseEntity.ok(
                ResultHandler.success(
                        "Dictionary review successfully",
                        HttpStatus.OK.value(),
                        result
                )
        );
    }

    @Operation(
            summary = "Add a new order status",
            description = "Creates a new order status in the system. The English name is automatically used to generate a unique token. Requires MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Order status created successful",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error or order status name already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    @PostMapping("/status/add")
    public ResponseEntity<ResultHandler<Void>> addStatus(@RequestBody @Valid AddEntityRequest request) {
        _orderServices.addStatus(request);
        return ResponseEntity.ok(ResultHandler.success(
                "Order status created successful",
                HttpStatus.CREATED.value()
        ));
    }

    @Operation(
            summary = "Add a new order item status",
            description = "Creates a new order item status in the system. The English name is automatically used to generate a unique token. Requires MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Order status created successful",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error or Ingredient name already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    @PostMapping("/item/status/add")
    public ResponseEntity<ResultHandler<Void>> addItemStatus(@RequestBody @Valid AddEntityRequest request) {
        _orderServices.addItemStatus(request);
        return ResponseEntity.ok(ResultHandler.success(
                "Order item status created successful",
                HttpStatus.CREATED.value()
        ));
    }
}
