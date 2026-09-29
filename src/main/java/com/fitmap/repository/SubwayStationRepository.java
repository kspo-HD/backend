package com.fitmap.repository;

import com.fitmap.domain.region.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SubwayStationRepository extends JpaRepository<SubwayStation, Long> {

    @Query(value = """
        SELECT name, route,
               ST_Distance(geom, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS dist_m
        FROM subway_stations
        WHERE geom IS NOT NULL
          AND ST_DWithin(geom, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusM)
        ORDER BY dist_m
        LIMIT 5
        """, nativeQuery = true)
    List<Object[]> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusM") int radiusM);
}
