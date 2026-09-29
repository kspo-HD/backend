package com.fitmap.domain.region;

import jakarta.persistence.*;
import lombok.Getter;
import java.math.BigDecimal;

@Entity
@Table(name = "region_rental")
@Getter
public class RegionRental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sido;

    @Column(name = "avg_rent_1f")
    private BigDecimal avgRent1f;

    private String quarter;
}
