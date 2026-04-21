package com.solarl.nado.config;

import com.solarl.nado.entity.User;
import com.solarl.nado.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// работает только при app.admin.seed-enabled=true
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(String... args) {
        if (!adminProperties.isSeedEnabled()) {
            log.debug("Admin seeding disabled (app.admin.seed-enabled=false)");
            return;
        }

        if (userRepository.findByEmail(adminProperties.getEmail()).isEmpty()) {
            User admin = User.builder()
                    .name("Администратор")
                    .email(adminProperties.getEmail())
                    .passwordHash(passwordEncoder.encode(adminProperties.getPassword()))
                    .role(User.Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("Admin account initialized: {}", adminProperties.getEmail());
        }
    }
}
