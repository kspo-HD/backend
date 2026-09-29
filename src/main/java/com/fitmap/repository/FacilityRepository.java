package com.fitmap.repository;

import com.fitmap.domain.facility.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    // 반경 내 시설 조회 (PostGIS ST_DWithin)
    @Query(value = """
        SELECT * FROM facilities
        WHERE ST_DWithin(
            geom,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            :radiusM
        )
        AND (CAST(:category AS VARCHAR) IS NULL OR category = CAST(:category AS VARCHAR))
        AND status = '정상운영'
        ORDER BY ST_Distance(geom, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)
        LIMIT 200
        """, nativeQuery = true)
    List<Facility> findNearby(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusM") int radiusM,
        @Param("category") String category
    );

    // 반경 내 전체 시설 (폐업 포함, 점수 계산용)
    @Query(value = """
        SELECT * FROM facilities
        WHERE ST_DWithin(
            geom,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            :radiusM
        )
        ORDER BY ST_Distance(geom, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)
        LIMIT 500
        """, nativeQuery = true)
    List<Facility> findAllNearby(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusM") int radiusM
    );
}
