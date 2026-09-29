package com.fitmap.domain.region;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "region_single_household")
@Getter
public class RegionSingleHousehold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sido;

    @Column(name = "total_single_hh", nullable = false)
    private Integer totalSingleHh;

    @Column(name = "single_hh_20_29", nullable = false)
    private Integer singleHh2029;

    @Column(name = "single_hh_30_39", nullable = false)
    private Integer singleHh3039;

    @Column(name = "single_hh_40_49", nullable = false)
    private Integer singleHh4049;

    @Column(name = "single_hh_2049", nullable = false)
    private Integer singleHh2049;

    @Column(name = "base_month")
    private String baseMonth;
}
