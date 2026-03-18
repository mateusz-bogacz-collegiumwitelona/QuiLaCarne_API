package com.example.restaurant.repository;

import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaDishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DishRepository implements IDishRepository {
    private final IJpaDishRepository _jpaDishRepo;
    private final DishMapper _dishMapper;

    @Value("${application.storage.s3.public-endpoint}")
    private String s3Endpoint;

    @Value("${application.storage.s3.bucket-name}")
    private String s3BucketName;

    @Override
    public PagedResult<DishListResponse> findAllDishes(String lang, DishFilterRequest request, PaggedRequest pagged) {
        Pageable pageable = PageRequest.of(pagged.getPage() - 1, pagged.getSize());
        Page<Dishes> dishPage;

        var excludedAllergens = request.getExcludedAllergens();

        if (excludedAllergens != null && !excludedAllergens.isEmpty()) {
            dishPage = _jpaDishRepo.findWithoutAllergens(excludedAllergens, pageable);
        } else {
            dishPage = _jpaDishRepo.findAll(pageable);
        }

        Page<DishListResponse> dtoPage = dishPage.map(dish -> {
            DishListResponse dto = _dishMapper.toDishListResponse(dish, lang);

            if (dto.getImageUrl() != null && !dto.getImageUrl().startsWith("http")) {
                String fullUrl = String.format("%s/%s/%s", s3Endpoint.trim(), s3BucketName, dto.getImageUrl());
                dto.setImageUrl(fullUrl);
            }

            return dto;
        });

        return new PagedResult<>(dtoPage);
    }

    @Override
    public boolean isDishExist(String token) {
        return _jpaDishRepo.findByToken(token).isPresent();
    }
}
