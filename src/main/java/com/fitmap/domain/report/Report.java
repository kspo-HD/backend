package com.fitmap.domain.report;

import com.fitmap.domain.analysis.Analysis;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Report {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    private Short score;
    private Character grade;

    @Column(name = "competitor_count")
    private Integer competitorCount;

    @Column(name = "closure_rate")
    private BigDecimal closureRate;

    @Column(name = "public_ratio")
    private BigDecimal publicRatio;

    @Column(name = "summary_json", columnDefinition = "jsonb")
    private String summaryJson;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "data_snapshot_at")
    private LocalDateTime dataSnapshotAt;

    @Column(name = "is_paid")
    private Boolean isPaid = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
}
