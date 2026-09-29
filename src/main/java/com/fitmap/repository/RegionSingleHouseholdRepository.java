package com.fitmap.repository;

import com.fitmap.domain.region.RegionSingleHousehold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionSingleHouseholdRepository extends JpaRepository<RegionSingleHousehold, Long> {
    Optional<RegionSingleHousehold> findBySido(String sido);
}
