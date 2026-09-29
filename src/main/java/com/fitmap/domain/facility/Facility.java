package com.fitmap.domain.facility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "facilities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Facility {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String category;
    private String status;

    @Column(name = "closed_at")
    private LocalDate closedAt;

    private Double lat;
    private Double lng;

    @JsonIgnore
    @Column(columnDefinition = "geography(Point,4326)")
    private Point geom;

    private String sido;
    private String sigungu;

    @Column(name = "road_addr")
    private String roadAddr;

    private Short floor;

    @Column(name = "area_m2")
    private Integer areaM2;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "is_free")
    private Boolean isFree;

    @Column(name = "open_weekday")
    private String openWeekday;

    @Column(name = "open_weekend")
    private String openWeekend;

    private Integer capacity;
    private String source;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "kakao_place_id")
    private String kakaoPlaceId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
