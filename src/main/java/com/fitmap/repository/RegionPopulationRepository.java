package com.fitmap.repository;

import com.fitmap.domain.region.RegionPopulation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RegionPopulationRepository extends JpaRepository<RegionPopulation, Long> {
    Optional<RegionPopulation> findBySidoAndSigungu(String sido, String sigungu);
    Optional<RegionPopulation> findTopBySidoOrderByTotalPopDesc(String sido);
}
