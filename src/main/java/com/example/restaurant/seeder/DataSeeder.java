package com.example.restaurant.seeder;

import com.example.restaurant.helpers.TranslatedData;
import com.example.restaurant.models.*;
import com.example.restaurant.models.base.BaseNamedEntity;
import com.example.restaurant.models.base.BaseTranslatedEntity;
import com.example.restaurant.models.lookup.*;
import com.example.restaurant.repository.interfaces.jpa.*;
import com.example.restaurant.repository.interfaces.jpa.base.IJpaTranslatedRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    private final IJpaRoleRepository _jpaRoleRepo;
    private final IJpaTableStatusRepository _jpaTableStatusRepo;
    private final IJpaOrederStatusRepositry _jpaOrederStatusRepo;
    private final IJpaReservationStatusRepository _jpaReservationStatusRepo;
    private final IJpaOrderItemStatusRepository _jpaOrederItemStatusRepo;
    private final IJpaUserRepository _jpaUserRepos;
    private final IJpaBanStatusRepository _jpaBanStatusRepo;
    private final IJpaGuestReportStatusRepository _jpaGuestReportStatusRepo;
    private final IJpaAllergensRepository _jpaAllergensRepo;
    private final IJpaDishesCategoryRepository _jpaDishesCategoryRepo;
    private final IJpaDishRepository _jpaDishRepo;
    private final IJpaIngredientsRepository _jpaIngredientsRepo;
    private final PasswordEncoder _passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Start data seeding...");

        seedNamedEntity(_jpaRoleRepo, Roles::new, List.of("ROLE_MANAGER", "ROLE_WAITER", "ROLE_CLIENT"));

        seedTranslatedEntity(_jpaTableStatusRepo, TableStatus::new, List.of(
                new TranslatedData("AVAILABLE", "Wolny", "Available"),
                new TranslatedData("RESERVED", "Zarezerwowany", "Reserved"),
                new TranslatedData("OCCUPIED", "Zajęty", "Occupied"),
                new TranslatedData("OUT_OF_SERVICE", "Wyłączony z użytku", "Out of service")
        ));

        seedTranslatedEntity(_jpaOrederStatusRepo, OrderStatus::new, List.of(
                new TranslatedData("PENDING", "Oczekujące", "Pending"),
                new TranslatedData("IN_PROGRESS", "W realizacji", "In progress"),
                new TranslatedData("SERVED", "Podano", "Served"),
                new TranslatedData("CANCELLED", "Anulowano", "Cancelled")
        ));

        seedTranslatedEntity(_jpaDishesCategoryRepo, DishesCategories::new, List.of(
                new TranslatedData("STARTER", "Przystawki", "Starters"),
                new TranslatedData("SOUP", "Zupy", "Soups"),
                new TranslatedData("MAIN", "Dania główne", "Main courses"),
                new TranslatedData("DESSERT", "Desery", "Desserts"),
                new TranslatedData("DRINK", "Napoje", "Drinks")
        ));

        seedTranslatedEntity(_jpaAllergensRepo, Allergens::new, List.of(
                new TranslatedData("GLUTEN", "Gluten", "Gluten"),
                new TranslatedData("LACTOSE", "Laktoza", "Lactose"),
                new TranslatedData("NUTS", "Orzechy", "Nuts")
        ));

        seedTranslatedEntity(_jpaReservationStatusRepo, ReservationStatus::new, List.of(
                new TranslatedData("ACTIVE", "Aktywna", "Active"),
                new TranslatedData("COMPLETED", "Zakończona", "Completed"),
                new TranslatedData("CANCELLED", "Anulowana", "Cancelled"),
                new TranslatedData("NO_SHOW", "Nieobecność", "No show")
        ));

        seedTranslatedEntity(_jpaOrederStatusRepo, OrderStatus::new, List.of(
                new TranslatedData("ACTIVE", "Aktywna", "Active"),
                new TranslatedData("COMPLETED", "Zakończona", "Completed"),
                new TranslatedData("CANCELLED", "Anulowana", "Cancelled")
        ));

        seedTranslatedEntity(_jpaBanStatusRepo, BanStatus::new, List.of(
                new TranslatedData("ACCEPTED", "Zaakceptowane", "Accepted"),
                new TranslatedData("REJECTED", "Odrzucone", "Rejected")
        ));

        seedTranslatedEntity(_jpaGuestReportStatusRepo, GuestReportStatus::new, List.of(
                new TranslatedData("ACCEPTED", "Zaakceptowane", "Accepted"),
                new TranslatedData("REJECTED", "Odrzucone", "Rejected")
        ));

        createMenu();

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
            List<TranslatedData> data
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
        user.setEmail(name + "@example.pl");
        user.setPassword(_passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setTwoFactorEnabled(false);
        user.setRoles(Set.of(userRole));

        _jpaUserRepos.save(user);
        log.info("User created: {} with role: {}", name, roleName);
    }

    private void createMenu()
    {
        if (_jpaDishRepo.count() > 0 ) return;

        Allergens gluten = _jpaAllergensRepo.findByToken("GLUTEN").orElseThrow();
        Allergens lactose  = _jpaAllergensRepo.findByToken("LACTOSE").orElseThrow();
        Allergens nuts = _jpaAllergensRepo.findByToken("NUTS").orElseThrow();

        Ingredients beef = createIngredient("Wołowina Chianina", "Chianina Beef", "beef-chianina", Set.of());
        Ingredients tomato = createIngredient("Pomidory San Marzano", "San Marzano Tomatoes", "tomatoes-sm", Set.of());
        Ingredients mozzarella = createIngredient("Mozzarella di Bufala", "Buffalo Mozzarella", "mozzarella-bufala", Set.of(lactose));
        Ingredients pasta = createIngredient("Makaron Tagliatelle", "Tagliatelle Pasta", "tagliatelle-pasta", Set.of(gluten));
        Ingredients parmesan = createIngredient("Ser Grana Padano", "Grana Padano Cheese", "grana-padano", Set.of(lactose));
        Ingredients oliveOil = createIngredient("Oliwa z oliwek", "Olive Oil", "olive-oil", Set.of());

        DishesCategories startCat = _jpaDishesCategoryRepo.findByToken("STARTER").orElseThrow();
        DishesCategories mainCat = _jpaDishesCategoryRepo.findByToken("MAIN").orElseThrow();

        Dishes steak = new Dishes();
        steak.setName("Bistecca alla Fiorentina");
        steak.setPrice(12000);
        steak.setCategory(mainCat);
        steak.setAvailable(true);
        steak.setIngredients(Set.of(beef, oliveOil));
        _jpaDishRepo.save(steak);

        Dishes pastaDish = new Dishes();
        pastaDish.setName("Tagliatelle Ragu");
        pastaDish.setPrice(4200);
        pastaDish.setCategory(mainCat);
        pastaDish.setAvailable(true);
        pastaDish.setIngredients(Set.of(pasta, beef, tomato, parmesan));
        _jpaDishRepo.save(pastaDish);
    }

    private Ingredients createIngredient(String pl, String en, String token, Set<Allergens> allergens) {
        Ingredients ing = new Ingredients();
        ing.setNamePl(pl);
        ing.setNameEn(en);
        ing.setToken(token);
        ing.setAllergens(allergens);
        return _jpaIngredientsRepo.save(ing);
    }
}
