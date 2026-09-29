package com.fitmap.domain.region;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "region_population")
@Getter
public class RegionPopulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sido;

    @Column(nullable = false)
    private String sigungu;

    @Column(name = "total_pop", nullable = false)
    private Long totalPop;

    @Column(name = "pop_20_49")
    private Long pop2049;
}
