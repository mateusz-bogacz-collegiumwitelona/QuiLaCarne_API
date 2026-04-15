package com.example.restaurant.commands;

import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.jpa.IJpaRoleRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminCommands implements CommandLineRunner {
    private final IJpaUserRepository _userRepo;
    private final IJpaRoleRepository _roleRepo;
    private final PasswordEncoder _passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private static final Pattern PASS_UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern PASS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern PASS_SPECIAL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    @Override
    public void run(String... args) {
        if (args.length > 0 && Arrays.asList(args).contains("--create-admin")) {
            Scanner scanner = new Scanner(System.in);

            log.info("Enter username:");
            String username = scanner.nextLine().trim();
            if (username.isBlank()) {
                log.error("Error: Username cannot be empty");
                return;
            }

            if (_userRepo.findByUsername(username).isPresent()) {
                log.warn("Error: User with this name '{}' already exist", username);
                return;
            }

            log.info("Enter email: ");
            String email = scanner.nextLine().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                log.error("Error: The email you provided has an incorrect structure");
                return;
            }

            log.info("Enter password (min. 8 characters, capital letter, number, special character): ");
            String password = scanner.nextLine().trim();
            if (!validatePassword(password)) return;

            createAdmin(username, email, password);
        }
    }

    private boolean validatePassword(String password) {
        if (password.length() < 8) {
            log.error("Error: Password must be at least 8 characters long");
            return false;
        }

        if (!PASS_UPPERCASE.matcher(password).find()) {
            log.error("Error: Password must contain at least one uppercase letter");
            return false;
        }

        if (!PASS_DIGIT.matcher(password).find()) {
            log.error("Error: Password must contain at least one number");
            return false;
        }

        if (!PASS_SPECIAL.matcher(password).find()) {
            log.error("Error: Password must contain at least one special character");
            return false;
        }

        return true;
    }

    private void createAdmin(String username, String email, String password) {

        Roles adminRole = _roleRepo.findByName("ROLE_MANAGER")
                .orElseThrow(() -> new RuntimeException("Role not exists"));

        Users admin = new Users();
        admin.setUsername(username);
        admin.setNormalizedUsername(username.toUpperCase().trim());
        admin.setEmail(email);
        admin.setNormalizedEmail(email.toUpperCase().trim());
        admin.setPassword(_passwordEncoder.encode(password));
        admin.setIsActive(true);
        admin.setRoles(Set.of(adminRole));

        _userRepo.save(admin);

        log.info("Admin created successfully: {}", username);
    }
}
