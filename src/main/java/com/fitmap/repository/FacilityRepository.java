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

    @Query(value = """
        SELECT lat, lng, name, category, is_public
        FROM facilities
        WHERE lat IS NOT NULL AND lng IS NOT NULL
        AND (CAST(:sido AS VARCHAR) IS NULL OR sido = :sido)
        AND (CAST(:category AS VARCHAR) IS NULL OR category = :category)
        AND (CAST(:isPublic AS VARCHAR) IS NULL OR is_public = CAST(:isPublic AS BOOLEAN))
        LIMIT 5000
        """, nativeQuery = true)
    List<Object[]> findForMap(
        @Param("sido") String sido,
        @Param("category") String category,
        @Param("isPublic") String isPublic
    );

    @Query("SELECT COUNT(f) FROM Facility f WHERE f.status = '정상운영'")
    long countActive();

    @Query("SELECT COUNT(f) FROM Facility f WHERE f.sido = :sido AND f.category = :category AND f.status = '정상운영'")
    long countActiveBySidoAndCategory(@Param("sido") String sido, @Param("category") String category);

    @Query("SELECT COUNT(f) FROM Facility f WHERE f.sido = :sido AND f.status = '정상운영'")
    long countActiveBySido(@Param("sido") String sido);

    @Query("SELECT f.category, COUNT(f) FROM Facility f GROUP BY f.category ORDER BY COUNT(f) DESC")
    List<Object[]> countByCategory();

    @Query("SELECT f.type, COUNT(f) FROM Facility f WHERE f.type IS NOT NULL GROUP BY f.type ORDER BY COUNT(f) DESC")
    List<Object[]> countByType();

    @Query("SELECT f.sido, COUNT(f) FROM Facility f WHERE f.sido IS NOT NULL GROUP BY f.sido ORDER BY COUNT(f) DESC")
    List<Object[]> countBySido();

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
