package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.*;
import com.example.restaurant.dto.response.*;
import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.dto.sync.SyncDishResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.mappers.DishMapper;
import com.example.restaurant.mappers.SyncMapper;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class DishServices implements IDishServices {
    private final IDishRepository _dishRepo;
    private final DishMapper _dishMapper;
    private final IIngredientsRepository _ingredientsRepo;
    private final S3StorageService _s3Services;
    private final NotificationServices _notification;

    private final SyncMapper _syncMapper;

    private static final String CATEGORY_ENTITY_TYPE = "DISH_CATEGORY";
    private static final String DISH_ENTITY_TYPE = "DISH";

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

        int pageIndex = Math.max(0, pagged.getPage() - 1);
        Pageable pageable = PageRequest.of(pageIndex, pagged.getSize());

        Page<Dishes> dishPage;
        var excludedAllergens = request.getExcludedAllergens();

        if (excludedAllergens != null && !excludedAllergens.isEmpty()) {
            dishPage = _dishRepo.findWithoutAllergens(excludedAllergens, pageable);
        } else {
            dishPage = _dishRepo.findAll(pageable);
        }

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
    @Caching(evict = {
            @CacheEvict(value = "dishMenu", allEntries = true),
            @CacheEvict(value = "publicDishMenu", allEntries = true)
    })
    public void remove(String dishToken) {
        Dishes dish = _dishRepo.findByToken(dishToken);

        dish.setUnavailableReason("Dish is deleted");
        dish.setAvailable(false);
        dish.setDeletedAt(OffsetDateTime.now());

        _s3Services.deleteFile(dish.getImageUrl());
        dish.setImageUrl(null);

        _dishRepo.save(dish);

        WebSocketEvent<Void> event = WebSocketEvent.deleted(DISH_ENTITY_TYPE, dishToken);
        _notification.sendEventToTopic("/menu/dishes", event);
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_DISH_AVAILABLE")
    @Caching(evict = {
        @CacheEvict(value = "dishMenu", allEntries = true),
        @CacheEvict(value = "publicDishMenu", allEntries = true)
    })
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

        WebSocketEvent<SyncDishResponse> event = WebSocketEvent.updated(
                DISH_ENTITY_TYPE,
                dish.getToken(),
                _syncMapper.toSyncDishResponse(dish)
        );

        _notification.sendEventToTopic("/menu/dishes", event);
    }

    @Override
    @Transactional
    @Auditable(action = "EDIT_DISH")
    @Caching(evict = {
            @CacheEvict(value = "dishMenu", allEntries = true),
            @CacheEvict(value = "publicDishMenu", allEntries = true)
    })
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

        WebSocketEvent<SyncDishResponse> event = WebSocketEvent.updated(
                DISH_ENTITY_TYPE,
                dish.getToken(),
                _syncMapper.toSyncDishResponse(dish)
        );
        _notification.sendEventToTopic("/menu/dishes", event);
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

        _dishRepo.save(dish);

        WebSocketEvent<SyncDishResponse> event = WebSocketEvent.created(
                DISH_ENTITY_TYPE,
                dish.getToken(),
                _syncMapper.toSyncDishResponse(dish)
        );
        _notification.sendEventToTopic("/menu/dishes", event);
    }

    @Override
    @Cacheable(value = "dishCategories",
            key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
    public DictionaryResponse getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return new DictionaryResponse(DictionaryHelper.map(_dishRepo.findAllCategories(), lang));
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
        SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(category);
        WebSocketEvent<SyncDictionaryResponse> event = WebSocketEvent.created(CATEGORY_ENTITY_TYPE, category.getToken(), payload);
        _notification.sendEventToTopic("/dictionary/dish-categories", event);
    }

    @Override
    @Transactional
    @Auditable(action = "REMOVE_DISH_CATEGORY")
    @Caching(evict = {
            @CacheEvict(value = "dishCategories", allEntries = true),
            @CacheEvict(value = "dishMenu", allEntries = true),
            @CacheEvict(value = "publicDishMenu", allEntries = true)
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

        WebSocketEvent<Void> event = WebSocketEvent.deleted(CATEGORY_ENTITY_TYPE, token);
        _notification.sendEventToTopic("/dictionary/dish-categories", event);
    }

    @Override
    @Cacheable(
            value = "publicDishMenu",
            key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public PublicMenuResponse getPublicMenu() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        List<Dishes> allDishes = _dishRepo.findAll();

        List<MenuResponse> menu = allDishes.stream()
                .filter(Dishes::isAvailable)
                .collect(Collectors.groupingBy(
                        d -> d.getCategory().translate(lang)
                ))
                .entrySet()
                .stream()
                .map( d -> {
                    String category = d.getKey();

                    List<DishResponse> dishesInCategory = d.getValue()
                            .stream()
                            .map(dish -> mapToDishResponse(dish, lang))
                            .toList();

                    return MenuResponse
                            .builder()
                            .category(category)
                            .dish(dishesInCategory)
                            .build();
                } ).toList();

        return new PublicMenuResponse(menu);
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
            } catch (IOException e) {
                log.error("Error reading photo input stream", e);
                throw new RuntimeException("Could not process photo file", e);
            }
        }
    }

    private DishResponse mapToDishResponse(Dishes dish, String lang) {
        List<String> ingridents = dish.getIngredients().stream()
                .map(i -> i.translate(lang))
                .toList();

        List<String> allergens = dish.getIngredients().stream()
                .flatMap(i -> i.getAllergens().stream())
                .map(a -> a.translate(lang))
                .distinct()
                .toList();

        String imageUrl = "";
        if (s3Endpoint == null || s3Endpoint.isBlank() || s3BucketName == null) {
            log.error("S3 storage is not properly configured. Returning relative image path.");
        } else {
            imageUrl = String.format("%s/%s/%s", s3Endpoint.trim(), s3BucketName, dish.getImageUrl());
        }

        return DishResponse.builder()
                .name(dish.getName())
                .price(dish.getPrice())
                .ingridents(ingridents)
                .imageUrl(imageUrl)
                .allergens(allergens)
                .build();
    }
}
