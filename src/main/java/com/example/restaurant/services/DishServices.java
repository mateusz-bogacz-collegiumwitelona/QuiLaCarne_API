package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.DishListResponse;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.helpers.PagedResult;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    private final NotificationServices _notification;

    @Value("${application.storage.s3.public-endpoint}")
    private String s3Endpoint;

    @Value("${application.storage.s3.bucket-name}")
    private String s3BucketName;

    @Override
    @Cacheable(value = "dishMenu",
            key = "#request.toString() + '-' + #pagged.toString() + '-' + " +
                    "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
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
    @CacheEvict(value = "dishMenu", allEntries = true)
    public void remove(String dishToken) {
        Dishes dish = _dishRepo.findByToken(dishToken);

        dish.setUnavailableReason("Dish is deleted");
        dish.setAvailable(false);
        dish.setDeletedAt(OffsetDateTime.now());

        _s3Services.deleteFile(dish.getImageUrl());

        dish.setImageUrl(null);

        _notification.sendToTopic("menu/updates", "Dish removed: " + dishToken);

        _dishRepo.save(dish);
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_DISH_AVAILABLE")
    @CacheEvict(value = "dishMenu", allEntries = true)
    public void changeAvailable(ChangeDishAvailableRequest request) {
        Dishes dish = _dishRepo.findByToken(request.getToken());

        dish.setAvailable(request.isAvailable());

        if (request.isAvailable()) {
            dish.setUnavailableReason(null);
        } else {
            String reason = request.getUnavailableReason();
            dish.setUnavailableReason(reason != null && !reason.isBlank() ? reason.trim() : "Brak składników");
        }

        _notification.sendToTopic(
                "menu",
                "Dish " + request.getToken() + " is now " +
                        (request.isAvailable() ? "available" : "unavailable")
        );
        _dishRepo.save(dish);
    }

    @Override
    @Transactional
    @Auditable(action = "EDIT_DISH")
    @CacheEvict(value = "dishMenu", allEntries = true)
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

        _notification.sendToTopic("menu/updates", "Dish updated: " + request.getDishToken());

        _dishRepo.save(dish);
    }

    @Override
    @Transactional
    @Auditable(action = "ADD_DISH")
    @CacheEvict(value = "dishMenu", allEntries = true)
    public void add(AddDishRequest request) {
        Dishes dish = new Dishes();

        dish.setName(request.getName().trim());
        dish.setPrice(request.getPrice());

        DishesCategories category = _dishRepo.findCategoryByToken(request.getCategoryToken());
        dish.setCategory(category);

        dish.setAvailable(true);
        updateDishIngredients(dish, request.getIngredientTokens());
        updateDishPhoto(dish, request.getPhoto());

        _notification.sendToTopic("menu/updates", "New dish added");

        _dishRepo.save(dish);
    }

    @Override
    @Cacheable(value = "dishCategories",
            key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
    public List<EntityResponse> getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return DictionaryHelper.map(_dishRepo.findAllCategories(), lang);
    }


    @Override
    @Transactional
    @Auditable(action = "ADD_DISH_CATEGORY")
    @CacheEvict(value = "dishCategories", allEntries = true)
    public void addCategory(AddEntityRequest request) {
        DishesCategories category = DictionaryHelper.createEntity(
                DishesCategories::new,
                request,
                _dishRepo::isCategoryNameTaken,
                "Dish category already exists"
        );

        _dishRepo.saveCategory(category);
        _notification.sendToTopic("dictionary/sync", "dish_categories");
    }

    @Override
    @Transactional
    @Auditable(action = "REMOVE_DISH_CATEGORY")
    @Caching(evict = {
            @CacheEvict(value = "dishCategories", allEntries = true),
            @CacheEvict(value = "dishMenu", allEntries = true)
    })
    public void removeCategory(String token) {
        DictionaryHelper.deleteEntity(
                token,
                _dishRepo::findCategoryByToken,
                _dishRepo::saveCategory,
                c -> {
                    DishesCategories fallback = _dishRepo.findCategoryByToken("OTHER");

                    List<Dishes> affected = _dishRepo.findByCategoryId(c.getId());

                    for (Dishes dish : affected) {
                        dish.setCategory(fallback);
                        _dishRepo.save(dish);
                    }
                }
        );

        _notification.sendToTopic("dictionary/sync", "dish_categories");
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
