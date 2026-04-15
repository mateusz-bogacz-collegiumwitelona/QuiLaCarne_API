package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IIngredientsServices;
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

@RestController
@RequestMapping(value = "/api/ingredients", produces = "application/json")
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class IngredientsController {
    private final IIngredientsServices _ingredientsServices;

    @Operation(
            summary = "Add a new ingredient",
            description = "Creates a new ingredient in the system. " +
                    "The English name is automatically used to generate a unique token. Requires MANAGER role.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Ingredient created successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error or Ingredient name already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    @PostMapping
    public ResponseEntity<ResultHandler<Void>> add(@RequestBody @Valid AddIngredientRequest request) {
        _ingredientsServices.add(request);
        return ResponseEntity.ok(ResultHandler.success(
                "Ingredient created successful",
                HttpStatus.CREATED.value()
        ));
    }

    @Operation(
            summary = "Remove an ingredient",
            description = "Performs a soft delete on an ingredient by its token. " +
                    "Also automatically marks all dishes containing this ingredient as unavailable. " +
                    "Requires MANAGER role.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ingredient removed successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role"),
            @ApiResponse(responseCode = "404", description = "Ingredient not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    @DeleteMapping("/{token}")
    public ResponseEntity<ResultHandler<Void>> remove(@PathVariable String token) {
        _ingredientsServices.remove(token);
        return ResponseEntity.ok(ResultHandler.success(
                "Ingredient remove successfuly",
                HttpStatus.OK.value()
        ));
    }

    @Operation(
            summary = "Get list of ingredients (dictionary)",
            description = "Retrieves a dictionary list of all ingredients available in the system. " +
                    "The names are translated based on the 'Accept-Language' header.",
            tags = {"Manager", "Client", "Waiter"}
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
                    description = "Dictionary review successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/dictionary")
    public ResponseEntity<ResultHandler<DictionaryResponse>> getDictionary() {
        var result = _ingredientsServices.getDictionary();
        return ResponseEntity.ok(
                ResultHandler.success(
                        "Dictionary review successfully",
                        HttpStatus.OK.value(),
                        result
                )
        );
    }
}
