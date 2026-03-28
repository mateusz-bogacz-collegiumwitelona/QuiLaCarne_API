package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.AddIngredientRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IIngredientsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/ingredients", produces = "application/json")
@RequiredArgsConstructor
public class IngredientsController {
    private final IIngredientsServices _ingredientsServices;

    @Operation(
            summary = "Add a new ingredient",
            description = "Creates a new ingredient in the system. The English name is automatically used to generate a unique token. Requires MANAGER role."
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
    @PostMapping("/add")
    public ResponseEntity<ResultHandler<Void>> add(@RequestBody @Valid AddIngredientRequest request) {
        var result = _ingredientsServices.add(request);
        return ResponseEntity.status(result.getStatusCode()).body(result);
    }
}
