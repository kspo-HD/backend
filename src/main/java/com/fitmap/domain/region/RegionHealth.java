package com.fitmap.domain.region;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(
    name = "region_health",
    uniqueConstraints = @UniqueConstraint(columnNames = {"sido", "sigungu", "survey_year"})
)
@Getter
public class RegionHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sido;

    private String sigungu;

    @Column(name = "aerobic_rate")
    private Double aerobicRate;

    @Column(name = "walking_rate")
    private Double walkingRate;

    @Column(name = "obesity_rate")
    private Double obesityRate;

    @Column(name = "survey_year", nullable = false)
    private Integer surveyYear;
}
