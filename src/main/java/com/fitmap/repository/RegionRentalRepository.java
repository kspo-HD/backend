package com.fitmap.repository;

import com.fitmap.domain.region.RegionRental;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RegionRentalRepository extends JpaRepository<RegionRental, Long> {
    Optional<RegionRental> findBySido(String sido);
}
