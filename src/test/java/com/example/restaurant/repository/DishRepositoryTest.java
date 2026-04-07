package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.DishFilterRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.lookup.DishesCategories;
import com.example.restaurant.repository.interfaces.jpa.IJpaDishRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaDishesCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DishRepositoryTest {
    @Mock
    private IJpaDishRepository _jpaDishRepo;

    @Mock
    private IJpaDishesCategoryRepository _jpaDishCategoryRepo;

    @InjectMocks
    private DishRepository _dishRepo;

    @Test
    @DisplayName("Find all dishes: should return full if no allergens excluded")
    void findAllDishes_ShouldCallJpaFindAll_WhenNoAllergensExcluded() {
        Dishes dishes = new Dishes();
        dishes.setName("Pizza");

        DishFilterRequest filterRequest = new DishFilterRequest();
        PaggedRequest paggedRequest = new PaggedRequest();
        paggedRequest.setPage(1);
        paggedRequest.setSize(10);

        Pageable expectedPageable = PageRequest.of(0, 10);
        Page<Dishes> mockPage = new PageImpl<>(List.of(dishes), expectedPageable, 1);

        when(_jpaDishRepo.findAll(expectedPageable)).thenReturn(mockPage);

        Page<Dishes> result = _dishRepo.findAllDishes(filterRequest, paggedRequest);

        assertEquals(1, result.getContent().size());
        assertEquals("Pizza", result.getContent().get(0).getName());

        verify(_jpaDishRepo, times(1)).findAll(expectedPageable);
    }

    @Test
    @DisplayName("Find all dishes: should return list without excluded allergens")
    void findAllDishes_ShouldCallJpaFindWithoutAllergens_WhenAllergensAreExcluded() {
        Dishes dishes = new Dishes();
        dishes.setName("Salad");

        DishFilterRequest filter = new DishFilterRequest();
        filter.setExcludedAllergens(List.of("GLUTEN"));

        PaggedRequest pagged = new PaggedRequest();
        pagged.setPage(2);
        pagged.setSize(5);

        Pageable expectedPageable = PageRequest.of(1, 5);
        Page<Dishes> mockPage = new PageImpl<>(List.of(dishes), expectedPageable, 10);

        when(_jpaDishRepo.findWithoutAllergens(List.of("GLUTEN"), expectedPageable)).thenReturn(mockPage);

        Page<Dishes> result = _dishRepo.findAllDishes(filter, pagged);

        assertEquals(1, result.getContent().size());
        assertEquals("Salad", result.getContent().get(0).getName());

        verify(_jpaDishRepo, times(1))
                .findWithoutAllergens(List.of("GLUTEN"), expectedPageable);
        verify(_jpaDishRepo, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List for orders: return list of dishes")
    void listForOrder_ShouldReturnListOfDishes() {
        List<String> tokens = List.of("TOKEN1", "TOKEN2");
        List<Dishes> expectedDishes = List.of(new Dishes(), new Dishes());

        when(_jpaDishRepo.findAllByTokenIn(tokens)).thenReturn(expectedDishes);

        List<Dishes> result = _dishRepo.listForOrder(tokens);

        assertEquals(2, result.size());
        verify(_jpaDishRepo, times(1)).findAllByTokenIn(tokens);
    }

    @Test
    @DisplayName("Get: should return dish if exist")
    void get_ShouldReturnDish_WhenTokenExists() {
        Dishes d1 = new Dishes();
        d1.setToken("T1");
        Dishes d2 = new Dishes();
        d2.setToken("T2");

        Dishes result = _dishRepo.get(List.of(d1, d2), "T2");

        assertEquals("T2", result.getToken());
    }

    @Test
    @DisplayName("Get: Throw exception when dish doesn't exists")
    void get_ShouldThrowException_WhenTokenDoesNotExist() {
        assertThrows(RuntimeException.class, () -> _dishRepo.get(List.of(), "MISSING"));
    }

    @Test
    @DisplayName("Find dish by ingridient id: should return full list of dishes")
    void findByIngredientsId_ShouldReturnListOfDishes() {
        UUID id = UUID.randomUUID();
        List<Dishes> expectedDishes = List.of(new Dishes(), new Dishes());
        when(_jpaDishRepo.findByIngredientsId(id)).thenReturn(expectedDishes);

        List<Dishes> result = _dishRepo.findByIngredientsId(id);

        assertEquals(2, result.size());
        verify(_jpaDishRepo).findByIngredientsId(id);
    }

    @Test
    @DisplayName("find By Token: Solving the problem when a token exists")
    void findByToken_ShouldReturnDish_WhenExists() {
        String token = "VALID_TOKEN";
        Dishes expectedDish = new Dishes();
        expectedDish.setToken(token);

        when(_jpaDishRepo.findByToken(token)).thenReturn(Optional.of(expectedDish));

        Dishes result = _dishRepo.findByToken(token);

        assertNotNull(result);
        assertEquals(token, result.getToken());
        verify(_jpaDishRepo, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("find By Token: Should throw EntityNotFoundException when token does not exist")
    void findByToken_ShouldThrowEntityNotFoundException_WhenDoesNotExist() {
        String token = "INVALID_TOKEN";
        when(_jpaDishRepo.findByToken(token)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> _dishRepo.findByToken(token)
        );

        assertEquals("Dish not found", exception.getMessage());
        verify(_jpaDishRepo, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("save: should correctly trigger a write to the JPA repository")
    void save_ShouldCallJpaSave() {

        Dishes dish = new Dishes();
        dish.setName("Pasta");

        _dishRepo.save(dish);

        verify(_jpaDishRepo, times(1)).save(dish);
    }

    @Test
    @DisplayName("findCategoryByToken: Should return category when token exists")
    void findCategoryByToken_ShouldReturnCategory_WhenExists() {
        String token = "CAT_TOKEN";
        DishesCategories expectedCategory = new DishesCategories();
        when(_jpaDishCategoryRepo.findByToken(token)).thenReturn(Optional.of(expectedCategory));

        DishesCategories result = _dishRepo.findCategoryByToken(token);

        assertNotNull(result);
        verify(_jpaDishCategoryRepo, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("findCategoryByToken: Should throw EntityNotFoundException when category does not exist")
    void findCategoryByToken_ShouldThrowException_WhenNotFound() {
        String token = TestConstants.TOKEN_NON_EXISTENT;
        when(_jpaDishCategoryRepo.findByToken(token)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> _dishRepo.findCategoryByToken(token)
        );

        assertEquals("Dish category not found", exception.getMessage());
        verify(_jpaDishCategoryRepo, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("findAllCategories: Should return list of categories from JPA")
    void findAllCategories_ShouldReturnListOfCategories() {
        List<DishesCategories> expectedCategories = List.of(new DishesCategories(), new DishesCategories());

        when(_jpaDishCategoryRepo.findAll()).thenReturn(expectedCategories);

        List<DishesCategories> result = _dishRepo.findAllCategories();

        assertEquals(expectedCategories, result);
        verify(_jpaDishCategoryRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("isCategoryNameTaken: Should return true and short-circuit when PL name exists")
    void isCategoryNameTaken_ShouldReturnTrue_WhenPlNameExists() {
        when(_jpaDishCategoryRepo.findByNamePl(anyString()))
                .thenReturn(Optional.of(new DishesCategories()));

        boolean result = _dishRepo.isCategoryNameTaken("Zupy PL", "Soups EN");

        assertTrue(result);
        verify(_jpaDishCategoryRepo, times(1)).findByNamePl("Zupy PL");
        verify(_jpaDishCategoryRepo, never()).findByNameEn(anyString());
    }

    @Test
    @DisplayName("isCategoryNameTaken: Should return true when EN name exists")
    void isCategoryNameTaken_ShouldReturnTrue_WhenEnNameExists() {
        when(_jpaDishCategoryRepo.findByNamePl(anyString())).thenReturn(Optional.empty());
        when(_jpaDishCategoryRepo.findByNameEn(anyString()))
                .thenReturn(Optional.of(new DishesCategories()));

        boolean result = _dishRepo.isCategoryNameTaken("Zupy PL", "Soups EN");

        assertTrue(result);
        verify(_jpaDishCategoryRepo, times(1)).findByNamePl("Zupy PL");
        verify(_jpaDishCategoryRepo, times(1)).findByNameEn("Soups EN");
    }

    @Test
    @DisplayName("isCategoryNameTaken: Should return false when both names are available")
    void isCategoryNameTaken_ShouldReturnFalse_WhenBothNamesAreAvailable() {
        when(_jpaDishCategoryRepo.findByNamePl(anyString())).thenReturn(Optional.empty());
        when(_jpaDishCategoryRepo.findByNameEn(anyString())).thenReturn(Optional.empty());

        boolean result = _dishRepo.isCategoryNameTaken("Zupy PL", "Soups EN");

        assertFalse(result);
    }

    @Test
    @DisplayName("saveCategory: Should call JPA save")
    void saveCategory_ShouldCallJpaSave() {
        DishesCategories category = new DishesCategories();

        _dishRepo.saveCategory(category);

        verify(_jpaDishCategoryRepo, times(1)).save(category);
    }

    @Test
    @DisplayName("findByCategoryId: Should return list of dishes")
    void findByCategoryId_ShouldReturnListOfDishes() {
        UUID id = UUID.randomUUID();
        List<Dishes> expectedDishes = List.of(new Dishes(), new Dishes());
        when(_jpaDishRepo.findByCategoryId(id)).thenReturn(expectedDishes);

        List<Dishes> result = _dishRepo.findByCategoryId(id);

        assertEquals(2, result.size());
        verify(_jpaDishRepo).findByCategoryId(id);
    }
}