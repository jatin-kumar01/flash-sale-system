package com.flashsale.auth.config;

import com.flashsale.auth.entity.User;
import com.flashsale.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.enabled:true}")
    private boolean adminEnabled;

    @Value("${app.admin.email:admin@flashsale.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.admin.first-name:Admin}")
    private String firstName;

    @Value("${app.admin.last-name:Kumar}")
    private String lastName;

    @Override
    public void run(String... args) {

        if (!adminEnabled) {
            log.info("Admin seeding is disabled.");
            return;
        }

        String email = adminEmail.toLowerCase().trim();

        Optional<User> existingOpt = userRepository.findByEmail(email);
        if (existingOpt.isPresent()) {
            User existingUser = existingOpt.get();
            if (existingUser.getRoles() == null) {
                existingUser.setRoles(new HashSet<>(Set.of("ROLE_ADMIN")));
                userRepository.save(existingUser);
                log.info("Added ROLE_ADMIN to existing user: {}", email);
            } else if (!existingUser.getRoles().contains("ROLE_ADMIN")) {
                existingUser.getRoles().add("ROLE_ADMIN");
                userRepository.save(existingUser);
                log.info("Added ROLE_ADMIN to existing user: {}", email);
            } else {
                log.info("Admin user already exists with ROLE_ADMIN: {}", email);
            }
            return;
        }

        User admin = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .roles(new HashSet<>(Set.of("ROLE_ADMIN")))
                .build();

        userRepository.save(admin);

        log.info("========================================");
        log.info("Admin user created successfully");
        log.info("Email: {}", email);
        log.info("Role: ROLE_ADMIN");
        log.info("========================================");
    }
}