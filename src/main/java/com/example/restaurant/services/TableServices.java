package com.example.restaurant.services;

import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.services.interfaces.ITableServices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableServices implements ITableServices {
    private final ITableRespository _tableRepo;

    @Override
    public ResultHandler<List<TableListResponse>> getTables(OffsetDateTime startTime, OffsetDateTime endTime)
    {
        try {
            if (startTime == null || endTime == null)
                return ResultHandler.failure(
                    "Dates cannot be null",
                    HttpStatus.BAD_REQUEST.value()
            );

            OffsetDateTime now = OffsetDateTime.now();

            if (startTime.isAfter(endTime) ||
                    startTime.isEqual(endTime)
            )
                return ResultHandler.failure(
                        "Start time must be before end time",
                        HttpStatus.BAD_REQUEST.value()
                );

            if (startTime.isBefore(now.plusMinutes(30)))
                return ResultHandler.failure(
                        "Reservations must be made at least 30 minutes in advance",
                        HttpStatus.BAD_REQUEST.value()
                );

            if (startTime.isAfter(now.plusDays(60)))
                return ResultHandler.failure(
                        "Reservations can only be made up to 60 days in advance",
                        HttpStatus.BAD_REQUEST.value()
                );


            String lang = LocaleContextHolder.getLocale().getLanguage();

            var result = _tableRepo.findAllTables(lang, startTime, endTime);

            return ResultHandler.success(
                    "Tables retrived",
                    HttpStatus.OK.value(),
                    result
            );
        }
        catch (Exception ex) {
            return ResultHandler.failure(
                    ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }
}
