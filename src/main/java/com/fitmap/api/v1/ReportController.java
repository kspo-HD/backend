package com.fitmap.api.v1;

import com.fitmap.common.ApiResponse;
import com.fitmap.domain.region.RegionHealth;
import com.fitmap.domain.report.Report;
import com.fitmap.repository.RegionHealthRepository;
import com.fitmap.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final RegionHealthRepository regionHealthRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(reportService.list(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@PathVariable UUID id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        Report report = reportService.get(id, userId);

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

        // 지역 건강 지표 (주소 기반)
        String address = report.getAnalysis().getAddress();
        if (address != null && !address.isBlank()) {
            String[] parts = address.trim().split("\\s+");
            String sido    = parts.length > 0 ? parts[0] : null;
            String sigungu = parts.length > 1 ? parts[1] : null;
            Optional<RegionHealth> health = Optional.empty();
            if (sido != null && sigungu != null) {
                health = regionHealthRepository.findBySidoAndSigunguAndSurveyYear(sido, sigungu, 2025);
            }
            if (health.isEmpty() && sido != null) {
                health = regionHealthRepository.findBySidoAndSigunguIsNullAndSurveyYear(sido, 2025);
            }
            health.ifPresent(h -> {
                body.put("aerobicRate",  h.getAerobicRate());
                body.put("walkingRate",  h.getWalkingRate());
                body.put("obesityRate",  h.getObesityRate());
                body.put("healthRegion", sido + (sigungu != null ? " " + sigungu : ""));
            });
        }

        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/{id}/competitors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> competitors(@PathVariable UUID id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(reportService.getCompetitors(id, userId)));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unlock(@PathVariable UUID id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(reportService.unlock(id, userId)));
    }
}
