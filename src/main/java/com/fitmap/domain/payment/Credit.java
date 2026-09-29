package com.fitmap.domain.payment;

import com.fitmap.domain.report.Report;
import com.fitmap.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Credit {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Report report;              // null = 미사용

    @Column(name = "used_at")
    private LocalDateTime usedAt;       // null = 미사용
}
