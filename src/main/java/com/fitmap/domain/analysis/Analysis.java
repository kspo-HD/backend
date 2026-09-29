package com.fitmap.domain.analysis;

import com.fitmap.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analyses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Analysis {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String category;
    private Double lat;
    private Double lng;
    private String address;

    @Column(name = "radius_m")
    private Integer radiusM;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
}
