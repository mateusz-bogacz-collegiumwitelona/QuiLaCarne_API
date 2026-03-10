package com.example.restaurant.services;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.services.interfaces.IDishServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.context.i18n.LocaleContextHolder;


@Service
@RequiredArgsConstructor
public class DishServices implements IDishServices {
    private final IDishRepository _dishRepo;

    @Override
    public ResultHandler<PagedResult<DishListResponse>> getMenu(DishFilterRequest request, PaggedRequest pagged) {
        try {
            String lang = LocaleContextHolder.getLocale().getLanguage();

            var result = _dishRepo.findAllDishes(lang, request, pagged);

            return ResultHandler.success(
                    "Menu retrived",
                    HttpStatus.OK.value(),
                    result);
        }
        catch (Exception ex) {
            return ResultHandler.failure(
                    ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }
}
