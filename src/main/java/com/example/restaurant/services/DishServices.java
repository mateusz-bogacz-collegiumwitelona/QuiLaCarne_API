package com.example.restaurant.services;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.services.interfaces.IDishServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class DishServices implements IDishServices {
    private final IDishRepository _dishRepo;
    private final DishMapper _dishMapper;

    @Value("${application.storage.s3.public-endpoint}")
    private String s3Endpoint;

    @Value("${application.storage.s3.bucket-name}")
    private String s3BucketName;

    @Override
    public ResultHandler<PagedResult<DishListResponse>> getMenu(DishFilterRequest request, PaggedRequest pagged) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        var dishPage = _dishRepo.findAllDishes(request, pagged);

        Page<DishListResponse> result = dishPage.map(d -> {
            DishListResponse dto = _dishMapper.toDishListResponse(d, lang);

            if (dto.getImageUrl() != null && !dto.getImageUrl().startsWith("http")) {
                String fullUrl = String.format("%s/%s/%s", s3Endpoint.trim(), s3BucketName, dto.getImageUrl());
                dto.setImageUrl(fullUrl);
            }

            return dto;
        });

        return ResultHandler.success(
                "Menu retrived",
                HttpStatus.OK.value(),
                new PagedResult<>(result));
    }
}
