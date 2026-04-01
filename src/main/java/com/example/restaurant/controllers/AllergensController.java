package com.example.restaurant.controllers;

import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.IAllergensServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/allergens", produces = "application/json")
@RequiredArgsConstructor
public class AllergensController {
    private final IAllergensServices _allergensServices;

    @Operation(
            summary = "Get list of allergens",
            description = "Retrieves a dictionary list of all allergens available in the system. The names are translated based on the 'Accept-Language' header."
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
        var result = _allergensServices.getDictionary();
        return ResponseEntity.ok(
                ResultHandler.success(
                        "Dictionary review successfully",
                        HttpStatus.OK.value(),
                        result
                )
        );
    }

}
