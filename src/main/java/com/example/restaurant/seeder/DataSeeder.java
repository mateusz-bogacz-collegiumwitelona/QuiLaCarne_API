package com.example.restaurant.seeder;

import com.example.restaurant.models.*;
import com.example.restaurant.models.base.BaseNamedEntity;
import com.example.restaurant.repository.interfaces.jpa.*;
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
    private final PasswordEncoder _passwordEncoder;

    @Override
    @Transactional
    public void run(String... args){
        log.info("Start data seeding...");

        seedNamedEntity(_jpaRoleRepo, Roles::new, List.of("ROLE_MANAGER", "ROLE_WAITER", "ROLE_CLIENT"));
        seedNamedEntity(_jpaTableStatusRepo, TableStatus::new, List.of("AVAILABLE", "RESERVED", "OCCUPIED", "OUT_OF_SERVICE"));
        seedNamedEntity(_jpaOrederStatusRepo, OrderStatus::new, List.of("PENDING", "IN_PROGRESS", "SERVED", "CANCELLED"));
        seedNamedEntity(_jpaReservationStatusRepo, ReservationStatus::new, List.of("ACTIVE", "COMPLETED", "CANCELLED", "NO_SHOW"));
        seedNamedEntity(_jpaOrederItemStatusRepo, OrderItemsStatus::new, List.of("PENDING", "PREPARING", "SERVED", "CANCELLED"));

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
            log.info("Seed table: {}", names.get(0).getClass().getSimpleName());
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
}
