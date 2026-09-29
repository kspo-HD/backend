package com.fitmap.domain.report;

import com.fitmap.domain.facility.Facility;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "report_competitors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportCompetitor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "distance_m")
    private Integer distanceM;
}
