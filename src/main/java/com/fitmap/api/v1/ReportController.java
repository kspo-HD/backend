package com.fitmap.api.v1;

import com.fitmap.domain.facility.Facility;
import com.fitmap.domain.report.Report;
import com.fitmap.domain.report.ReportCompetitor;
import com.fitmap.repository.CreditRepository;
import com.fitmap.repository.ReportCompetitorRepository;
import com.fitmap.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportRepository reportRepository;
    private final ReportCompetitorRepository reportCompetitorRepository;
    private final CreditRepository creditRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        List<Map<String, Object>> result = reportRepository.findByUserId(userId).stream()
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
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        Report report = reportRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", report.getId());
        body.put("score", report.getScore());
        body.put("grade", String.valueOf(report.getGrade()));
        body.put("competitorCount", report.getCompetitorCount());
        body.put("closureRate", report.getClosureRate());
        body.put("publicRatio", report.getPublicRatio());
        body.put("isPaid", report.getIsPaid());
        body.put("createdAt", report.getCreatedAt());
        body.put("category", report.getAnalysis().getCategory());
        body.put("lat", report.getAnalysis().getLat());
        body.put("lng", report.getAnalysis().getLng());
        body.put("radiusM", report.getAnalysis().getRadiusM());

        if (Boolean.TRUE.equals(report.getIsPaid())) {
            body.put("summaryJson", report.getSummaryJson());
            body.put("locked", false);
        } else {
            body.put("summaryJson", null);
            body.put("locked", true);
        }

        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/competitors")
    public ResponseEntity<?> competitors(@PathVariable UUID id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        reportRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        List<Map<String, Object>> result = reportCompetitorRepository
            .findByReport_IdOrderByDistanceMAsc(id).stream()
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

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<?> unlock(@PathVariable UUID id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        Report report = reportRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (Boolean.TRUE.equals(report.getIsPaid())) {
            return ResponseEntity.ok(Map.of("message", "already_unlocked", "summaryJson", report.getSummaryJson()));
        }

        var credit = creditRepository.findOldestUnused(userId)
            .orElseThrow(() -> new IllegalStateException("크레딧이 부족합니다"));

        credit.setReport(report);
        credit.setUsedAt(LocalDateTime.now());
        creditRepository.save(credit);

        report.setIsPaid(true);
        reportRepository.save(report);

        return ResponseEntity.ok(Map.of(
            "message", "unlocked",
            "summaryJson", report.getSummaryJson()
        ));
    }
}
