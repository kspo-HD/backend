package com.fitmap.service;

import com.fitmap.domain.facility.Facility;
import com.fitmap.domain.report.Report;
import com.fitmap.domain.report.ReportCompetitor;
import com.fitmap.repository.CreditRepository;
import com.fitmap.repository.ReportCompetitorRepository;
import com.fitmap.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportCompetitorRepository reportCompetitorRepository;
    private final CreditRepository creditRepository;

    public List<Map<String, Object>> list(Long userId) {
        return reportRepository.findByUserId(userId).stream()
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("analysisId", r.getAnalysis().getId());
                m.put("category", r.getAnalysis().getCategory());
                m.put("address", r.getAnalysis().getAddress());
                m.put("score", r.getScore());
                m.put("grade", String.valueOf(r.getGrade()));
                m.put("isPaid", r.getIsPaid());
                m.put("createdAt", r.getCreatedAt());
                return m;
            })
            .toList();
    }

    public Report get(UUID id, Long userId) {
        return reportRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    public List<Map<String, Object>> getCompetitors(UUID id, Long userId) {
        reportRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        return reportCompetitorRepository.findByReport_IdOrderByDistanceMAsc(id).stream()
            .map(c -> {
                Facility f = c.getFacility();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("facilityId", f.getId());
                m.put("name", f.getName());
                m.put("category", f.getCategory());
                m.put("status", f.getStatus());
                m.put("lat", f.getLat());
                m.put("lng", f.getLng());
                m.put("roadAddr", f.getRoadAddr());
                m.put("areaM2", f.getAreaM2());
                m.put("floor", f.getFloor());
                m.put("isPublic", f.getIsPublic());
                m.put("isFree", f.getIsFree());
                m.put("openWeekday", f.getOpenWeekday());
                m.put("distanceM", c.getDistanceM());
                return m;
            })
            .toList();
    }

    @Transactional
    public Map<String, Object> unlock(UUID id, Long userId) {
        Report report = reportRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (Boolean.TRUE.equals(report.getIsPaid())) {
            return Map.of("message", "already_unlocked", "summaryJson", report.getSummaryJson());
        }

        var credit = creditRepository.findOldestUnused(userId)
            .orElseThrow(() -> new IllegalStateException("크레딧이 부족합니다"));

        credit.setReport(report);
        credit.setUsedAt(LocalDateTime.now());
        creditRepository.save(credit);

        report.setIsPaid(true);
        reportRepository.save(report);

        return Map.of("message", "unlocked", "summaryJson", report.getSummaryJson());
    }
}
