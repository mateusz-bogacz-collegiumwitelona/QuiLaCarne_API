package com.example.restaurant.services;

import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.services.interfaces.ITableServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TableServices implements ITableServices {
    private final ITableRespository _tableRepo;

    @Override
    public ResultHandler<List<TableListResponse>> getTables(TableFilterRequest request) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        var result = _tableRepo.findAllTables(lang, request.getStartTime(), request.getEndTime());

        return ResultHandler.success(
                "Tables retrived",
                HttpStatus.OK.value(),
                result
        );
    }

    @Override
    @Transactional
    public ResultHandler<Void> changeStatusToClean(String tableToken) {
        String clearStatusToken = "CLEANING";

        _tableRepo.changeStatus(tableToken, clearStatusToken);

        return ResultHandler.success(
                "Status change successfully",
                HttpStatus.OK.value()
        );
    }
}
