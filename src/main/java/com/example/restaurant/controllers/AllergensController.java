package com.example.restaurant.controllers;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IAllergensServices;
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
@RequestMapping(value = "/api/dishes", produces = "application/json")
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class AllergensController {
    private final IAllergensServices _allergensServices;

    @Operation(
            summary = "Get list of allergens",
            description = "Retrieves a dictionary list of all allergens available in the system. " +
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
    @GetMapping("/allergens/dictionary")
    public ResponseEntity<ResultHandler<DictionaryResponse>> getAllergensDictionary() {
        var result = _allergensServices.getDictionary();
        return ResponseEntity.ok(
                ResultHandler.success(
                        "Dictionary review successfully",
                        HttpStatus.OK.value(),
                        result
                )
        );
    }

    @Operation(
            summary = "Add a new allergen",
            description = "Creates a new allergen in the system. " +
                    "The English name is automatically used to generate a unique token. " +
                    "Requires MANAGER role.",
            tags = {"Manager"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Allergen created successful",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error or Ingredient name already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token is required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_MANAGER role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    @PostMapping("/allergens/add")
    public ResponseEntity<ResultHandler<Void>> add(@RequestBody @Valid AddEntityRequest request) {
        _allergensServices.add(request);
        return ResponseEntity.ok(ResultHandler.success(
                "Allergen created successful",
                HttpStatus.CREATED.value()
        ));
    }

    @Operation(
            summary = "Remove a allergen (Soft Delete)",
            description = "Marks a dish as allergen by setting its availability to false, " +
                    "adding an unavailable reason, and setting the deleted_at timestamp. " +
                    "Requires ROLE_MANAGER privileges.",
            tags = {"Manager"}
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
                    description = "Allergen removed successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not logged in", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have the required ROLE_MANAGER role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Dish with the provided token does not exist", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping("/allergen/{token}")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<ResultHandler<Void>> removeAllergen(
            @PathVariable String token
    ) {
        _allergensServices.remove(token);

        return ResponseEntity.ok(ResultHandler.success(
                        "Allergen removed successfully",
                        HttpStatus.OK.value()
                )

        );
    }
}
