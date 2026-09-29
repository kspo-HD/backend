package com.fitmap.service;

import com.fitmap.domain.analysis.Analysis;
import com.fitmap.domain.facility.Facility;
import com.fitmap.domain.report.Report;
import com.fitmap.domain.report.ReportCompetitor;
import com.fitmap.domain.user.User;
import com.fitmap.repository.*;
import com.fitmap.service.ScoreCalculator.ScoreResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final FacilityRepository facilityRepository;
    private final AnalysisRepository analysisRepository;
    private final ReportRepository reportRepository;
    private final ReportCompetitorRepository reportCompetitorRepository;
    private final UserRepository userRepository;
    private final OpenAiReportService openAiReportService;

    @Transactional
    public Report createAnalysis(String userId, String category, double lat, double lng, int radiusM, String address) {
        User user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Facility> allNearby = facilityRepository.findAllNearby(lat, lng, radiusM);
        ScoreResult score = ScoreCalculator.calculate(allNearby, category);
        String summaryJson = openAiReportService.generateReport(category, radiusM, score, allNearby);

        Analysis analysis = Analysis.builder()
            .user(user).category(category).lat(lat).lng(lng).radiusM(radiusM).address(address)
            .build();
        analysisRepository.save(analysis);

        Report report = Report.builder()
            .analysis(analysis)
            .score((short) score.total())
            .grade((char) score.grade())
            .competitorCount(score.competitorCount())
            .closureRate(BigDecimal.valueOf(score.closureRate()))
            .publicRatio(BigDecimal.valueOf(score.publicRatio()))
            .summaryJson(summaryJson)
            .dataSnapshotAt(LocalDateTime.now())
            .isPaid(false)
            .build();
        reportRepository.save(report);

        // 상위 10개 경쟁사 저장 (거리 순, 동일 카테고리 운영 중)
        List<ReportCompetitor> competitors = allNearby.stream()
            .filter(f -> category.equals(f.getCategory())
                && "정상운영".equals(f.getStatus())
                && f.getLat() != null && f.getLng() != null)
            .limit(10)
            .map(f -> ReportCompetitor.builder()
                .report(report)
                .facility(f)
                .distanceM(haversineMeters(lat, lng, f.getLat(), f.getLng()))
                .build())
            .toList();
        reportCompetitorRepository.saveAll(competitors);

        return report;
    }

    private static int haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (int) (R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
