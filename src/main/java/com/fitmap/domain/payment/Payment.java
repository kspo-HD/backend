package com.fitmap.domain.payment;

import com.fitmap.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bundle_type", nullable = false)
    private Short bundleType;           // 1 | 3 | 5

    @Column(name = "total_credits", nullable = false)
    private Short totalCredits;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "pg_tx_id")
    private String pgTxId;

    @Column(name = "pg_provider")
    private String pgProvider;          // dummy | toss | iamport

    private String status;              // paid | failed | refunded

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
}
