package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.DictionaryMapper;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IIngredientsRepository;
import com.example.restaurant.services.interfaces.IDishServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Slf4j
public class DishServices implements IDishServices {
    private final IDishRepository _dishRepo;
    private final DishMapper _dishMapper;
    private final IIngredientsRepository _ingredientsRepo;
    private final S3StorageService _s3Services;

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

        _s3Services.deleteFile(dish.getImageUrl());

        dish.setImageUrl(null);

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

    @Override
    @Transactional
    @Auditable(action = "EDIT_DISH")
    public void edit(EditDishRequest request) {
        Dishes dish = _dishRepo.findByToken(request.getDishToken());

        if (request.getNewName() != null && !request.getNewName().isBlank())
            dish.setName(request.getNewName().trim());

        if (request.getPrice() != null) dish.setPrice(request.getPrice());

        if (request.getCategoryToken() != null) {
            DishesCategories category = _dishRepo.findCategoryByToken(request.getCategoryToken());
            dish.setCategory(category);
        }

        updateDishIngredients(dish, request.getIngredientTokens());
        updateDishPhoto(dish, request.getPhoto());
        _dishRepo.save(dish);
    }

    @Override
    @Transactional
    @Auditable(action = "ADD_DISH")
    public void add(AddDishRequest request) {
        Dishes dish = new Dishes();

        dish.setName(request.getName().trim());
        dish.setPrice(request.getPrice());

        DishesCategories category = _dishRepo.findCategoryByToken(request.getCategoryToken());
        dish.setCategory(category);

        dish.setAvailable(true);
        updateDishIngredients(dish, request.getIngredientTokens());
        updateDishPhoto(dish, request.getPhoto());

        _dishRepo.save(dish);
    }

    @Override
    public List<EntityResponse> getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return DictionaryMapper.map(_dishRepo.findAllCategories(), lang);
    }

    private void updateDishIngredients(Dishes dish, List<String> tokens) {
        if (!ObjectUtils.isEmpty(tokens)) {
            Set<Ingredients> newIngredients = new HashSet<>();
            for (String token : tokens) {
                Ingredients ingredient = _ingredientsRepo.findByToken(token);
                newIngredients.add(ingredient);
            }
            dish.setIngredients(newIngredients);
        }
    }

    private void updateDishPhoto(Dishes dish, MultipartFile photo) {
        if (photo != null && !photo.isEmpty()) {
            if (dish.getImageUrl() != null) _s3Services.deleteFile(dish.getImageUrl());

            String generatedName = _s3Services.generateUniqFileName(photo.getOriginalFilename());

            try {
                String finalFileName = _s3Services.uploadFromStream(
                        photo.getInputStream(),
                        generatedName,
                        photo.getContentType(),
                        photo.getSize()
                );
                dish.setImageUrl(finalFileName);
            } catch (java.io.IOException e) {
                log.error("Error reading photo input stream", e);
                throw new RuntimeException("Could not process photo file", e);
            }
        }
    }
}
