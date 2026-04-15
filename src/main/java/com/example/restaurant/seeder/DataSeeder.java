package com.example.restaurant.seeder;

import com.example.restaurant.dto.domain.TranslatedDomain;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Ingredients;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.base.BaseNamedEntity;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.models.lookup.*;
import com.example.restaurant.repository.interfaces.jpa.*;
import com.example.restaurant.repository.interfaces.jpa.base.IJpaTranslatedRepository;
import com.example.restaurant.services.S3StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    private final IJpaRoleRepository _jpaRoleRepo;
    private final IJpaTableStatusRepository _jpaTableStatusRepo;
    private final IJpaOrederStatusRepositry _jpaOrderStatusRepo;
    private final IJpaReservationStatusRepository _jpaReservationStatusRepo;
    private final IJpaOrderItemStatusRepository _jpaOrderItemStatusRepo;
    private final IJpaUserRepository _jpaUserRepos;
    private final IJpaBanStatusRepository _jpaBanStatusRepo;
    private final IJpaGuestReportStatusRepository _jpaGuestReportStatusRepo;
    private final IJpaAllergensRepository _jpaAllergensRepo;
    private final IJpaDishesCategoryRepository _jpaDishesCategoryRepo;
    private final IJpaDishRepository _jpaDishRepo;
    private final IJpaIngredientsRepository _jpaIngredientsRepo;
    private final PasswordEncoder _passwordEncoder;
    private final IJpaTableRepository _jpaTableRepo;
    private final S3StorageService _s3StorageService;
    private final ResourceLoader _resourceLoader;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Start data seeding...");

        seedNamedEntity(_jpaRoleRepo, Roles::new, List.of("ROLE_MANAGER", "ROLE_WAITER", "ROLE_CLIENT"));

        seedTranslatedEntity(_jpaTableStatusRepo, TableStatus::new, List.of(
                new TranslatedDomain("AVAILABLE", "Wolny", "Available"),
                new TranslatedDomain("RESERVED", "Zarezerwowany", "Reserved"),
                new TranslatedDomain("OCCUPIED", "Zajęty", "Occupied"),
                new TranslatedDomain("OUT_OF_SERVICE", "Wyłączony z użytku", "Out of service"),
                new TranslatedDomain("CLEANING", "Do sprzątnięcia", "Cleaning"),
                new TranslatedDomain("OTHER", "Inne", "Other")
        ));

        seedTranslatedEntity(_jpaOrderItemStatusRepo, OrderItemsStatus::new, List.of(
                new TranslatedDomain("PENDING", "Oczekujące", "Pending"),
                new TranslatedDomain("IN_PROGRESS", "W przygotowaniu", "In progress"),
                new TranslatedDomain("READY", "Gotowe do wydania", "Ready"),
                new TranslatedDomain("SERVED", "Wydane", "Served"),
                new TranslatedDomain("CANCELLED", "Anulowane", "Cancelled"),
                new TranslatedDomain("RETURNED", "Zwrócone", "Returned"),
                new TranslatedDomain("OTHER", "Inne", "Other")
        ));

        seedTranslatedEntity(_jpaDishesCategoryRepo, DishesCategories::new, List.of(
                new TranslatedDomain("STARTER", "Przystawki", "Starters"),
                new TranslatedDomain("SOUP", "Zupy", "Soups"),
                new TranslatedDomain("MAIN", "Dania główne", "Main courses"),
                new TranslatedDomain("DESSERT", "Desery", "Desserts"),
                new TranslatedDomain("DRINK", "Napoje", "Drinks"),
                new TranslatedDomain("OTHER", "Inne", "Other")
        ));

        seedTranslatedEntity(_jpaAllergensRepo, Allergens::new, List.of(
                new TranslatedDomain("GLUTEN", "Gluten", "Gluten"),
                new TranslatedDomain("LACTOSE", "Laktoza", "Lactose"),
                new TranslatedDomain("NUTS", "Orzechy", "Nuts"),
                new TranslatedDomain("EGGS", "Jaja", "Eggs"),
                new TranslatedDomain("SEAFOOD", "Owoce morza", "Seafood"),
                new TranslatedDomain("SOY", "Soja", "Soy")
        ));

        seedTranslatedEntity(_jpaReservationStatusRepo, ReservationStatus::new, List.of(
                new TranslatedDomain("ACTIVE", "Aktywna", "Active"),
                new TranslatedDomain("COMPLETED", "Zakończona", "Completed"),
                new TranslatedDomain("IN_PROGRESS", "W trakcie", "In progress"),
                new TranslatedDomain("CANCELLED", "Anulowana", "Cancelled"),
                new TranslatedDomain("NO_SHOW", "Nieobecność", "No show"),
                new TranslatedDomain("OTHER", "Inne", "Other")
        ));

        seedTranslatedEntity(_jpaOrderStatusRepo, OrderStatus::new, List.of(
                new TranslatedDomain("ACTIVE", "Aktywna", "Active"),
                new TranslatedDomain("COMPLETED", "Zakończona", "Completed"),
                new TranslatedDomain("IN_PROGRESS", "W trakcie", "In progress"),
                new TranslatedDomain("CANCELLED", "Anulowana", "Cancelled"),
                new TranslatedDomain("OTHER", "Inne", "Other")
        ));

        seedTranslatedEntity(_jpaBanStatusRepo, BanStatus::new, List.of(
                new TranslatedDomain("ACTIVE", "Aktywny", "ACTIVE"),
                new TranslatedDomain("EXPIRED", "Wygasły", "Expired"),
                new TranslatedDomain("REVOKED", "Cofinęty", "Revoked")
        ));

        seedTranslatedEntity(_jpaGuestReportStatusRepo, GuestReportStatus::new, List.of(
                new TranslatedDomain("IN_PROGRESS", "W trakcie", "In progress"),
                new TranslatedDomain("ACCEPTED", "Zaakceptowane", "Accepted"),
                new TranslatedDomain("REJECTED", "Odrzucone", "Rejected")
        ));

        createMenu();
        createTables();

        createAccount("client", "ROLE_CLIENT", "Client123!");
        createAccount("admin", "ROLE_MANAGER", "Admin123!");
        createAccount("waiter", "ROLE_WAITER", "Waiter123!");
    }

    private <T extends BaseNamedEntity> void seedNamedEntity(
            JpaRepository<T, ?> repo,
            Supplier<T> factory,
            List<String> names
    ) {
        if (repo.count() == 0) {
            names.forEach(name -> {
                T entity = factory.get();
                entity.setName(name);
                repo.save(entity);
            });
            log.info("Seed table: {} (added {} items)", factory.get().getClass().getSimpleName(), names.size());
        }
    }

    private <T extends BaseTranslatedEntity> void seedTranslatedEntity(
            IJpaTranslatedRepository<T> repo,
            Supplier<T> factory,
            List<TranslatedDomain> data
    ) {
        if (repo.count() == 0) {
            data.forEach(item -> {
                T entity = factory.get();
                entity.setToken(item.token());
                entity.setNamePl(item.pl());
                entity.setNameEn(item.en());
                repo.save(entity);
            });
            log.info("Seeded table: {} ({} items)", factory.get().getClass().getSimpleName(), data.size());
        }
    }

    private void createAccount(String name, String roleName, String password) {
        if (_jpaUserRepos.findByUsername(name).isPresent()) return;

        Roles userRole = _jpaRoleRepo.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Error: Role " + roleName + " not found"));

        Users user = new Users();
        user.setUsername(name);
        user.setNormalizedUsername(user.getUsername().toUpperCase().trim());
        user.setEmail(name + "@example.pl");
        user.setNormalizedEmail(user.getEmail().toUpperCase().trim());
        user.setPassword(_passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setIsTwoFactorEnabled(false);
        user.setRoles(Set.of(userRole));

        _jpaUserRepos.save(user);
        log.info("User created: {} with role: {}", name, roleName);
    }

    private void createMenu() {
        if (_jpaDishRepo.count() > 0) return;

        Allergens gluten = _jpaAllergensRepo.findByToken("GLUTEN").orElseThrow();
        Allergens lactose = _jpaAllergensRepo.findByToken("LACTOSE").orElseThrow();
        Allergens nuts = _jpaAllergensRepo.findByToken("NUTS").orElseThrow();
        Allergens eggs = _jpaAllergensRepo.findByToken("EGGS").orElseThrow();
        Allergens seafood = _jpaAllergensRepo.findByToken("SEAFOOD").orElseThrow();

        Ingredients beef = createIngredient("Wołowina Chianina", "Chianina Beef", "BEEF_CHIANINA", asSet());
        Ingredients tomato = createIngredient("Pomidory San Marzano", "San Marzano Tomatoes", "TOMATOES-SM", asSet());
        Ingredients mozzarella = createIngredient("Mozzarella di Bufala", "Buffalo Mozzarella", "MOZZARELLA-BUFFALA", asSet(lactose));
        Ingredients pasta = createIngredient("Makaron Tagliatelle", "Tagliatelle Pasta", "TAGLIATELLE-PASTA", asSet(gluten));
        Ingredients parmesan = createIngredient("Ser Grana Padano", "Grana Padano Cheese", "GRANA-PADANO", asSet(lactose));
        Ingredients oliveOil = createIngredient("Oliwa z oliwek", "Olive Oil", "OLIVE-OLI", asSet());
        Ingredients bread = createIngredient("Pieczywo domowe", "Homemade Bread", "BREAD-HOME", asSet(gluten));
        Ingredients shrimp = createIngredient("Krewetki tygrysie", "Tiger Prawns", "SHRIMPS-TIGER", asSet(seafood));
        Ingredients mascarpone = createIngredient("Serek Mascarpone", "Mascarpone Cheese", "MASCARPONE", asSet(lactose));
        Ingredients ladyfingers = createIngredient("Biszkopty", "Ladyfingers", "LADYFINGERS", asSet(gluten, eggs));
        Ingredients coffee = createIngredient("Kawa Espresso", "Espresso Coffee", "COFFEE-ESP", asSet());
        Ingredients rice = createIngredient("Ryż Arborio", "Arborio Rice", "RICE-ARBORIO", asSet());
        Ingredients lemon = createIngredient("Cytryny Sycylijskie", "Sicilian Lemons", "LEMON-SICILY", asSet());

        // NOWE SKŁADNIKI DLA PROBLEMÓW Z NUT / EGG
        Ingredients pistachios = createIngredient("Pistacje", "Pistachios", "PISTACHIOS", asSet(nuts));
        Ingredients eggIngredient = createIngredient("Jajka", "Eggs", "EGG_ING", asSet(eggs));

        DishesCategories starterCat = _jpaDishesCategoryRepo.findByToken("STARTER").orElseThrow();
        DishesCategories soupCat = _jpaDishesCategoryRepo.findByToken("SOUP").orElseThrow();
        DishesCategories mainCat = _jpaDishesCategoryRepo.findByToken("MAIN").orElseThrow();
        DishesCategories dessertCat = _jpaDishesCategoryRepo.findByToken("DESSERT").orElseThrow();
        DishesCategories drinkCat = _jpaDishesCategoryRepo.findByToken("DRINK").orElseThrow();
        DishesCategories otherCat = _jpaDishesCategoryRepo.findByToken("OTHER").orElseThrow();
        
        createDish("Bruschetta Classica", 2500, starterCat, asSet(bread, tomato, oliveOil), "bruschetta.jpg");
        createDish("Carpaccio di Manzo", 4800, starterCat, asSet(beef, parmesan, oliveOil), "carpaccio.jpg");
        createDish("Focaccia Rosmarino", 2200, starterCat, asSet(bread, oliveOil), "focaccia.jpg");

        createDish("Crema di Pomodoro", 2800, soupCat, asSet(tomato, mozzarella, oliveOil), "tomato_soup.jpg");
        createDish("Minestrone Toscano", 3200, soupCat, asSet(tomato, pasta), "minestrone.jpg");
        createDish("Zuppa di Pesce", 5500, soupCat, asSet(shrimp, tomato, oliveOil), "fish_soup.jpg");

        createDish("Bistecca alla Fiorentina", 12000, mainCat, asSet(beef, oliveOil), "steak.jpg");
        createDish("Tagliatelle Ragu", 4200, mainCat, asSet(pasta, beef, tomato, parmesan), "pasta.jpg");
        createDish("Risotto ai Funghi", 4500, mainCat, asSet(rice, parmesan, oliveOil), "risotto.jpg");

        createDish("Classic Tiramisu", 3500, dessertCat, asSet(mascarpone, ladyfingers, coffee), "tiramisu.jpg");
        createDish("Panna Cotta", 2800, dessertCat, asSet(mascarpone, lemon), "panna_cotta.jpg");
        createDish("Cannoli Siciliani", 3000, dessertCat, asSet(bread, mascarpone, pistachios), "cannoli.jpg");

        createDish("Espresso", 1200, drinkCat, asSet(coffee), "espresso.jpg");
        createDish("Limonata Fatta in Casa", 1800, drinkCat, asSet(lemon), "lemonade.jpg");
        createDish("Acqua Panna", 1500, drinkCat, asSet(), "water.jpg");

        createDish("Pane e Coperto", 1000, otherCat, asSet(bread), "bread_basket.jpg");
        createDish("Olive Marinate", 1500, otherCat, asSet(oliveOil), "olives.jpg");
        createDish("Salsa al Tartufo", 2000, otherCat, asSet(eggIngredient, oliveOil), "truffle_sauce.jpg");
    }

    @SafeVarargs
    private <T> Set<T> asSet(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    private void createDish(String name, int price, DishesCategories cat, Set<Ingredients> ing, String imgName) {
        Dishes dish = new Dishes();
        dish.setName(name);
        dish.setPrice(price);
        dish.setCategory(cat);
        dish.setAvailable(true);
        dish.setIngredients(ing);
        String url = uploadImage("images/" + imgName, imgName);
        dish.setImageUrl(url);
        _jpaDishRepo.save(dish);
    }

    private Ingredients createIngredient(String pl, String en, String token, Set<Allergens> allergens) {
        Ingredients ing = new Ingredients();
        ing.setNamePl(pl);
        ing.setNameEn(en);
        ing.setToken(token);
        ing.setAllergens(allergens);
        return _jpaIngredientsRepo.save(ing);
    }

    private void createTables() {
        if (_jpaTableRepo.count() > 0) return;

        TableStatus availableStatus = _jpaTableStatusRepo.findByToken("AVAILABLE")
                .orElseThrow(() -> new RuntimeException("Status AVAILABLE not found"));

        createSingleTable(1, 2, availableStatus);
        createSingleTable(2, 4, availableStatus);
        createSingleTable(3, 6, availableStatus);

        log.info("Seed: 3 restaurant table create");
    }

    private void createSingleTable(int number, int capacity, TableStatus status) {
        RestaurantTables table = new RestaurantTables();
        table.setTableNumber(number);
        table.setCapacity(capacity);

        table.setTableStatus(Set.of(status));
        _jpaTableRepo.save(table);
    }

    private String uploadImage(String resourcePath, String fileName) {
        try {
            var resource = _resourceLoader.getResource("classpath:" + resourcePath);

            if (resource.exists()) {
                return _s3StorageService.uploadFromStream(
                        resource.getInputStream(),
                        fileName,
                        "image/jpeg",
                        resource.contentLength()
                );
            } else {
                log.warn("Resource not found: {}", resourcePath);
                return null;
            }
        } catch (Exception e) {
            log.error("Error uploading image: {}", fileName, e);
            return null;
        }
    }
}
