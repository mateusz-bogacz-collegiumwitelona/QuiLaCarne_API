package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
public class UserFilterRequest {
    @Parameter(description = "Search by username or email")
    private String search;

    @Parameter(description = "Filtrowanie po roli, np. ROLE_MANAGER, ROLE_WAITER")
    private String role;

    @Parameter(description = "Activity filtering (true = active, false = blocked, null = all)")
    private Boolean isActive;

    @Parameter(description = "Field to sort (e.g. createdAt, username, email)")
    private String sortBy = "createdAt";

    @Parameter(description = "Sorting direction (ASC - ascending, DESC - descending)")
    private String sortDirection = "DESC";
}