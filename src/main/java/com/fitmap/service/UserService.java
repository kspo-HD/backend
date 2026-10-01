package com.fitmap.service;

import com.fitmap.domain.analysis.Analysis;
import com.fitmap.domain.report.Report;
import com.fitmap.repository.*;
import com.github.catomat0.oauthhelper.jwt.OahRefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AnalysisRepository analysisRepository;
    private final ReportRepository reportRepository;
    private final ReportCompetitorRepository reportCompetitorRepository;
    private final CreditRepository creditRepository;
    private final PaymentRepository paymentRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;
    private final OahRefreshTokenService refreshTokenService;

    @Transactional
    public void withdraw(Long userId) {
        // 1. Redis RT 삭제
        refreshTokenService.delete(String.valueOf(userId));

        // 2. 크레딧 삭제 (report_id, payment_id FK)
        creditRepository.deleteByUser_Id(userId);

        // 3. 리포트 경쟁사 → 리포트 순으로 삭제
        List<Analysis> analyses = analysisRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        if (!analyses.isEmpty()) {
            List<UUID> analysisIds = analyses.stream().map(Analysis::getId).toList();
            List<Report> reports = reportRepository.findByUserId(userId);
            if (!reports.isEmpty()) {
                List<UUID> reportIds = reports.stream().map(Report::getId).toList();
                reportCompetitorRepository.deleteByReport_IdIn(reportIds);
                reportRepository.deleteByAnalysis_IdIn(analysisIds);
            }
            analysisRepository.deleteAllInBatch(analyses);
        }

        // 4. 결제 삭제
        paymentRepository.deleteByUser_Id(userId);

        // 5. 약관 동의 삭제
        userTermsAgreementRepository.deleteByUser_Id(userId);

        // 6. 유저 삭제
        userRepository.deleteById(userId);
    }
}
