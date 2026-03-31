package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.ChangeDishAvailableRequest;
import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.services.interfaces.IDishServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class DishServices implements IDishServices {
    private final IDishRepository _dishRepo;
    private final DishMapper _dishMapper;

    @Value("${application.storage.s3.public-endpoint}")
    private String s3Endpoint;

    @Value("${application.storage.s3.bucket-name}")
    private String s3BucketName;

    @Override
    public PagedResult<DishListResponse> getMenu(DishFilterRequest request, PaggedRequest pagged) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        var dishPage = _dishRepo.findAllDishes(request, pagged);

        Page<DishListResponse> result = dishPage.map(d -> {
            DishListResponse dto = _dishMapper.toDishListResponse(d, lang);

            if (dto.getImageUrl() != null && !dto.getImageUrl().startsWith("http")) {
                if (s3Endpoint == null || s3Endpoint.isBlank() || s3BucketName == null) {
                    log.error("S3 storage is not properly configured. Returning relative image path.");
                } else {
                    String fullUrl = String.format("%s/%s/%s", s3Endpoint.trim(), s3BucketName, dto.getImageUrl());
                    dto.setImageUrl(fullUrl);
                }
            }

            return dto;
        });

        return new PagedResult<>(result);
    }

    @Override
    @Transactional
    @Auditable(action = "REMOVE_DISH")
    public void remove(String dishToken) {
        Dishes dish = _dishRepo.findByToken(dishToken);

        dish.setUnavailableReason("Dish is deleted");
        dish.setAvailable(false);
        dish.setDeletedAt(OffsetDateTime.now());

        _dishRepo.save(dish);
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_DISH_AVAILABLE")
    public void changeAvailable(ChangeDishAvailableRequest request) {
        Dishes dish = _dishRepo.findByToken(request.getToken());

        dish.setAvailable(request.isAvailable());

        if (request.isAvailable()) {
            dish.setUnavailableReason(null);
        } else {
            String reason = request.getUnavailableReason();
            dish.setUnavailableReason(reason != null && !reason.isBlank() ? reason.trim() : "Brak składników");
        }

        _dishRepo.save(dish);
    }
}
