package com.fitmap.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private String role = "USER";

    private String name;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(nullable = false)
    private String provider;          // kakao | naver | google

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "provider_access_token")
    private String providerAccessToken;

    @Column(name = "provider_refresh_token")
    private String providerRefreshToken;

    @Column(name = "terms_agreed_at")
    private LocalDateTime termsAgreedAt;

    @Column(name = "privacy_agreed_at")
    private LocalDateTime privacyAgreedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "interest_category")
    private String interestCategory;

    @Column(name = "interest_sido")
    private String interestSido;

    @Column(name = "budget_range")
    private String budgetRange;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
}
