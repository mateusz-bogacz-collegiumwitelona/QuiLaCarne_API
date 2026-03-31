package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IDishServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/dishes", produces = "application/json")
@RequiredArgsConstructor
public class DishController {
    private final IDishServices _dishServices;

    @Operation(
            summary = "Get full restaurant menu",
            description = "Returns a list of all available dishes including ingredients and allergens. " +
                    "The names of categories and allergens are translated based on the 'Accept-Language' header."
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
                    description = "Menu retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ResultHandler<PagedResult<DishListResponse>>> getMenu(
            @ParameterObject @Valid @ModelAttribute PaggedRequest pagged,
            @ParameterObject @ModelAttribute DishFilterRequest request
    ) {
        var result = _dishServices.getMenu(request, pagged);
        return ResponseEntity.ok(ResultHandler.success(
                "Menu retrived",
                HttpStatus.OK.value(),
                result));
    }

    @Operation(
            summary = "Remove a dish (Soft Delete)",
            description = "Marks a dish as deleted by setting its availability to false, adding an unavailable reason, and setting the deleted_at timestamp. Requires ROLE_MANAGER privileges."
    )
    @Parameter(
            name = "token",
            in = ParameterIn.PATH,
            description = "The unique token of the dish to be removed",
            required = true,
            schema = @Schema(type = "string")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dish removed successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have the required ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Dish with the provided token does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping("/{token}")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> remove(
            @PathVariable String token
    ) {
        _dishServices.remove(token);

        return ResponseEntity.ok(ResultHandler.success(
                        "Dish remove successfully",
                        HttpStatus.OK.value()
                )

        );
    }

    @Operation(
            summary = "Change dish availability",
            description = "Toggles the availability of a dish. If marking as unavailable, an optional reason can be provided. Requires ROLE_MANAGER privileges."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dish availability changed successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have the required ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Dish with the provided token does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PatchMapping
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> changeAvailable(
            @RequestBody @Valid ChangeDishAvailableRequest request
    ) {
        _dishServices.changeAvailable(request);
        return ResponseEntity.ok(
                ResultHandler.success(
                        "Dish available change successfull",
                        HttpStatus.OK.value()
                )
        );
    }

    @Operation(
            summary = "Edit an existing dish",
            description = "Updates dish details and/or replaces its photo on S3. Requires ROLE_MANAGER privileges. Uses multipart/form-data."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dish edited successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Dish not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> edit(
            @Valid @ModelAttribute EditDishRequest request
    ) {
        _dishServices.edit(request);

        return ResponseEntity.ok(
                ResultHandler.success(
                        "Dish edited successfully",
                        HttpStatus.OK.value()
                )
        );
    }

    @Operation(
            summary = "Add a new dish",
            description = "Creates a new dish in the menu and optionally uploads its photo to S3. Requires ROLE_MANAGER privileges. Uses multipart/form-data."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Dish created successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data or invalid file format", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have the required ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Category or Ingredient token does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> add(
            @Valid @ModelAttribute AddDishRequest request
    ) {
        _dishServices.add(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResultHandler.success(
                        "Dish created successfully",
                        HttpStatus.CREATED.value()
                )
        );
    }
}
