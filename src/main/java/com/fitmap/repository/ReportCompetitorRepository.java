package com.fitmap.repository;

import com.fitmap.domain.report.ReportCompetitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReportCompetitorRepository extends JpaRepository<ReportCompetitor, Long> {
    List<ReportCompetitor> findByReport_IdOrderByDistanceMAsc(UUID reportId);

    @Modifying
    @Query("DELETE FROM ReportCompetitor rc WHERE rc.report.id IN :reportIds")
    void deleteByReport_IdIn(@Param("reportIds") List<UUID> reportIds);
}
