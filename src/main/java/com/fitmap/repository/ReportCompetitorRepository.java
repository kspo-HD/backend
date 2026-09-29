package com.fitmap.repository;

import com.fitmap.domain.report.ReportCompetitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportCompetitorRepository extends JpaRepository<ReportCompetitor, Long> {
    List<ReportCompetitor> findByReport_IdOrderByDistanceMAsc(UUID reportId);
}
