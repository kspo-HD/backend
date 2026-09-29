package com.fitmap.domain.terms;

import com.fitmap.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_terms_agreements",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "terms_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTermsAgreement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id", nullable = false)
    private Terms terms;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;
}
