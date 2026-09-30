package com.fitmap.api.v1;

import com.fitmap.domain.analysis.Analysis;
import com.fitmap.domain.report.Report;
import com.fitmap.repository.AnalysisRepository;
import com.fitmap.service.AnalysisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisRepository analysisRepository;

    public record CreateAnalysisRequest(
        @NotBlank String category,
        @DecimalMin("-90") @DecimalMax("90") double lat,
        @DecimalMin("-180") @DecimalMax("180") double lng,
        @Min(300) @Max(5000) int radiusM,
        String address,
        String budgetRange
    ) {}

    public record AnalysisResponse(UUID id, String category, Double lat, Double lng, Integer radiusM, LocalDateTime createdAt) {
        static AnalysisResponse from(Analysis a) {
            return new AnalysisResponse(a.getId(), a.getCategory(), a.getLat(), a.getLng(), a.getRadiusM(), a.getCreatedAt());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateAnalysisRequest req, Authentication auth) {
        Report report = analysisService.createAnalysis(
            auth.getName(), req.category(), req.lat(), req.lng(), req.radiusM(), req.address(), req.budgetRange()
        );
        return ResponseEntity.ok(Map.of(
            "reportId", report.getId(),
            "score", report.getScore(),
            "grade", String.valueOf(report.getGrade()),
            "isPaid", report.getIsPaid()
        ));
    }

    @GetMapping
    public ResponseEntity<List<AnalysisResponse>> list(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        List<AnalysisResponse> result = analysisRepository.findByUser_IdOrderByCreatedAtDesc(userId)
            .stream().map(AnalysisResponse::from).toList();
        return ResponseEntity.ok(result);
    }
}
