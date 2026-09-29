package com.fitmap.domain.region;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "subway_stations")
@Getter
public class SubwayStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", unique = true)
    private String stationId;

    private String name;
    private String route;
    private Double lat;
    private Double lng;
    private String sido;
    private String sigungu;
}
