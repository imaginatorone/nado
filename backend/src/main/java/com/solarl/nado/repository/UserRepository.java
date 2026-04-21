package com.solarl.nado.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.solarl.nado.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakUserId(String keycloakUserId);
    boolean existsByEmail(String email);
}
