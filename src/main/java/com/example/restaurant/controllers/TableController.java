package com.example.restaurant.controllers;

import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.services.interfaces.ITableServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/tables", produces = "application/json")
@RequiredArgsConstructor
public class TableController {
    private final ITableServices _tableServices;

    @Operation(
            summary = "Get full list of restaurant tables",
            description = "Returns a list of all tables in restaurant. " +
                    "The names of table status are translated based on the 'Accept-Language' header."
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
                    description = "Table list retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ResultHandler.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ResultHandler<List<TableListResponse>>> getTables() {
        var result = _tableServices.getTables();

        return ResponseEntity.status(result.getStatusCode()).body(result);
    }
}
