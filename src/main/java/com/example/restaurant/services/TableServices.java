package com.example.restaurant.services;

import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.TableStatus;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.services.interfaces.ITableServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TableServices implements ITableServices {
    private final ITableRespository _tableRepo;

    @Override
    public ResultHandler<List<TableListResponse>> getTables(TableFilterRequest request) {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        boolean isAvailabilityCheck = (request.getStartTime() != null && request.getEndTime() != null);

        List<RestaurantTables> tables = _tableRepo.findAllTables(request.getStartTime(), request.getEndTime());

        List<TableListResponse> responses = tables.stream().map(table -> {
            String statusName = "UNKNOWN";
            if (table.getTableStatus() != null && !table.getTableStatus().isEmpty()) {
                var status = table.getTableStatus().iterator().next();
                statusName = "pl".equalsIgnoreCase(lang) ? status.getNamePl() : status.getNameEn();
            }
            if (isAvailabilityCheck)
                statusName = "pl".equalsIgnoreCase(lang) ? "Wolny" : "Available";

            return TableListResponse.builder()
                    .token(table.getToken())
                    .tableNumber(table.getTableNumber())
                    .capacity(table.getCapacity())
                    .status(statusName)
                    .updatedAt(table.getUpdatedAt())
                    .build();
        }).toList();

        return ResultHandler.success("Tables retrived",
                HttpStatus.OK.value(),
                responses
        );
    }

    @Override
    @Transactional
    public ResultHandler<Void> changeStatusToClean(String tableToken) {
        String clearStatusToken = "CLEANING";

        changeStatus(tableToken, clearStatusToken);

        return ResultHandler.success(
                "Status change successfully",
                HttpStatus.OK.value()
        );
    }

    private void changeStatus(String token, String statusToken) {
        RestaurantTables table = _tableRepo.findByToken(token);

        TableStatus cleanStatus = _tableRepo.findStatusByToken(statusToken);

        table.setTableStatus(new HashSet<>(Set.of(cleanStatus)));

        _tableRepo.save(table);
    }
}
