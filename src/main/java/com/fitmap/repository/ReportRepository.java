package com.fitmap.repository;

import com.fitmap.domain.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    @Query("SELECT r FROM Report r JOIN FETCH r.analysis a WHERE a.user.id = :userId ORDER BY r.createdAt DESC")
    List<Report> findByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Report r JOIN r.analysis a WHERE r.id = :reportId AND a.user.id = :userId")
    Optional<Report> findByIdAndUserId(@Param("reportId") UUID reportId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Report r WHERE r.analysis.id IN :analysisIds")
    void deleteByAnalysis_IdIn(@Param("analysisIds") List<UUID> analysisIds);
}
