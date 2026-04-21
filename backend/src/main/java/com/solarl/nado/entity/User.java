package com.solarl.nado.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(length = 100)
    private String region;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "completed_deals", nullable = false)
    @Builder.Default
    private Integer completedDeals = 0;

    @Column(name = "successful_deals", nullable = false)
    @Builder.Default
    private Integer successfulDeals = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer complaints = 0;

    // --- Keycloak integration ---

    @Column(name = "keycloak_user_id", unique = true)
    private String keycloakUserId;

    @Column(name = "auth_provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "trust_score")
    private Double trustScore;

    @Column(name = "is_banned", nullable = false)
    @Builder.Default
    private Boolean banned = false;

    // --- timestamps ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Equality by database identity (id), safe for JPA managed entities.
     * Two transient entities (id == null) are never equal unless same reference.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        // consistent hash for both transient and managed states
        return getClass().hashCode();
    }

    public enum Role {
        USER, MODERATOR, ADMIN
    }

    public enum AuthProvider {
        LOCAL, GOOGLE, VK
    }
}
