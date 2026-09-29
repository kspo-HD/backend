package com.fitmap.service;

import com.fitmap.domain.facility.Facility;

import java.util.List;

public class ScoreCalculator {

    public record ScoreResult(
        int total,
        char grade,
        int competitorCount,
        int closedCount,
        double closureRate,
        int publicCount,
        double publicRatio,
        int allActiveCount,
        // 세부 점수
        int competitionScore,
        int viabilityScore,
        int publicPressureScore,
        int areaVitalityScore,
        int demandScore
    ) {}

    public static ScoreResult calculate(List<Facility> allNearby, String targetCategory) {
        List<Facility> sameCategory = allNearby.stream()
            .filter(f -> targetCategory.equals(f.getCategory()))
            .toList();

        List<Facility> sameCategoryActive = sameCategory.stream()
            .filter(f -> "정상운영".equals(f.getStatus()))
            .toList();

        List<Facility> sameCategoryClosed = sameCategory.stream()
            .filter(f -> !"정상운영".equals(f.getStatus()))
            .toList();

        List<Facility> publicSameCategory = sameCategoryActive.stream()
            .filter(f -> Boolean.TRUE.equals(f.getIsPublic()) || Boolean.TRUE.equals(f.getIsFree()))
            .toList();

        List<Facility> allActive = allNearby.stream()
            .filter(f -> "정상운영".equals(f.getStatus()))
            .toList();

        int competitorCount = sameCategoryActive.size();
        int closedCount = sameCategoryClosed.size();
        int totalSame = sameCategory.size();
        int publicCount = publicSameCategory.size();
        int allActiveCount = allActive.size();

        double closureRate = totalSame == 0 ? 0.0 : (double) closedCount / totalSame;
        double publicRatio = (competitorCount + 1) == 0 ? 0.0 : (double) publicCount / (competitorCount + 1);

        // 1. 경쟁 강도 (30점) - 적을수록 유리
        int competitionScore = switch (competitorCount) {
            case 0 -> 30;
            case 1, 2 -> 27;
            case 3, 4, 5 -> 20;
            case 6, 7, 8, 9, 10 -> 12;
            default -> competitorCount <= 20 ? 5 : 2;
        };

        // 2. 시장 생존율 (25점) - 폐업률 낮을수록 유리
        int viabilityScore;
        if (closureRate == 0.0) viabilityScore = 25;
        else if (closureRate <= 0.10) viabilityScore = 22;
        else if (closureRate <= 0.20) viabilityScore = 16;
        else if (closureRate <= 0.30) viabilityScore = 10;
        else if (closureRate <= 0.50) viabilityScore = 4;
        else viabilityScore = 0;

        // 3. 공공시설 압박 (20점) - 무료 공공시설 적을수록 유리
        int publicPressureScore;
        if (publicCount == 0) publicPressureScore = 20;
        else if (publicRatio <= 0.10) publicPressureScore = 16;
        else if (publicRatio <= 0.25) publicPressureScore = 10;
        else if (publicRatio <= 0.50) publicPressureScore = 5;
        else publicPressureScore = 2;

        // 4. 상권 활성도 (15점) - 전체 시설 수가 상권 형성 지표
        int areaVitalityScore;
        if (allActiveCount >= 30) areaVitalityScore = 15;
        else if (allActiveCount >= 15) areaVitalityScore = 12;
        else if (allActiveCount >= 7) areaVitalityScore = 8;
        else if (allActiveCount >= 3) areaVitalityScore = 5;
        else areaVitalityScore = 2;

        // 5. 수요 신호 (10점) - 다양한 업종 = 유동인구 多
        int demandScore;
        if (competitorCount == 0 && allActiveCount == 0) {
            demandScore = 3; // 미개발지
        } else if (competitorCount == 0 && allActiveCount > 0) {
            demandScore = 9; // 잠재 블루오션
        } else {
            double diversity = (double) allActiveCount / competitorCount;
            if (diversity >= 8) demandScore = 10;
            else if (diversity >= 5) demandScore = 8;
            else if (diversity >= 3) demandScore = 5;
            else if (diversity >= 2) demandScore = 3;
            else demandScore = 1;
        }

        int total = competitionScore + viabilityScore + publicPressureScore + areaVitalityScore + demandScore;

        char grade;
        if (total >= 80) grade = 'S';
        else if (total >= 65) grade = 'A';
        else if (total >= 50) grade = 'B';
        else if (total >= 35) grade = 'C';
        else grade = 'D';

        return new ScoreResult(
            total, grade,
            competitorCount, closedCount, closureRate,
            publicCount, publicRatio, allActiveCount,
            competitionScore, viabilityScore, publicPressureScore, areaVitalityScore, demandScore
        );
    }
}
