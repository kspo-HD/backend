package com.fitmap.repository;

import com.fitmap.domain.region.RegionRental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface RegionRentalRepository extends JpaRepository<RegionRental, Long> {
    Optional<RegionRental> findBySido(String sido);

    @Query("SELECT AVG(r.avgRent1f) FROM RegionRental r")
    Double findNationalAvg();
}
