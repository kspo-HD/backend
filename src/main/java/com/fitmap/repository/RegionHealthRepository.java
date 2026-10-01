package com.fitmap.repository;

import com.fitmap.domain.region.RegionHealth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RegionHealthRepository extends JpaRepository<RegionHealth, Long> {
    Optional<RegionHealth> findBySidoAndSigunguAndSurveyYear(String sido, String sigungu, int year);
    Optional<RegionHealth> findBySidoAndSigunguIsNullAndSurveyYear(String sido, int year);
}
