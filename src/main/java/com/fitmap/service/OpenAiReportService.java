package com.fitmap.service;

import com.fitmap.domain.facility.Facility;
import com.fitmap.service.ScoreCalculator.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpenAiReportService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o}")
    private String model;

    private final RestClient restClient = RestClient.create();

    public String generateReport(String category, int radiusM, ScoreResult score, List<Facility> allNearby,
                                  String address, String budgetRange, String regionContext) {
        String prompt = buildPrompt(category, radiusM, score, allNearby, address, budgetRange, regionContext);

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.7,
            "response_format", Map.of("type", "json_object")
        );

        try {
            Map<?, ?> response = restClient.post()
                .uri(OPENAI_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            return (String) message.get("content");

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage());
            return fallbackJson(score, category);
        }
    }

    private String systemPrompt() {
        return """
            당신은 국내 피트니스·스포츠 창업 전문 컨설턴트입니다.
            실제 시설 데이터를 근거로 창업자에게 정확하고 실용적인 입지 분석을 제공합니다.
            반드시 JSON 형식만 반환하고, 분석은 한국어로 작성합니다.
            숫자와 데이터를 근거로 구체적인 인사이트를 제공하세요.
            막연한 표현보다는 '경쟁사 X개 중 Y개가 폐업'처럼 수치를 활용하세요.
            """;
    }

    private String buildPrompt(String category, int radiusM, ScoreResult score, List<Facility> allNearby,
                               String address, String budgetRange, String regionContext) {
        List<Facility> topCompetitors = allNearby.stream()
            .filter(f -> category.equals(f.getCategory()) && "정상운영".equals(f.getStatus()))
            .limit(5)
            .toList();

        String competitorList = topCompetitors.isEmpty()
            ? "- 반경 내 동일 업종 운영 시설 없음"
            : topCompetitors.stream()
                .map(f -> "- %s (%s, %s)".formatted(
                    f.getName(),
                    f.getRoadAddr() != null ? f.getRoadAddr() : f.getSigungu(),
                    f.getAreaM2() != null ? f.getAreaM2() + "㎡" : "규모 미상"
                ))
                .collect(Collectors.joining("\n"));

        String closedList = allNearby.stream()
            .filter(f -> category.equals(f.getCategory()) && !"정상운영".equals(f.getStatus()))
            .limit(3)
            .map(f -> "- %s (폐업)".formatted(f.getName()))
            .collect(Collectors.joining("\n"));
        if (closedList.isBlank()) closedList = "- 없음";

        String budgetContext = (budgetRange != null && !budgetRange.isBlank())
            ? "희망 창업 예산: " + budgetRange
            : "희망 창업 예산: 미지정";

        String regionCtx = (regionContext != null && !regionContext.isBlank())
            ? regionContext
            : "";

        return """
            [창업자 정보]
            창업 예정 지역: %s
            %s
            창업 예정 업종: %s
            분석 반경: %dm

            [입지 점수]
            총점: %d점 / %c등급
            - 경쟁 강도: %d/30점 (동일 업종 운영 시설 %d개)
            - 시장 생존율: %d/25점 (폐업률 %.1f%%, 폐업 시설 %d개)
            - 공공시설 압박: %d/20점 (무료·공공 시설 %d개)
            - 상권 활성도: %d/15점 (전체 운영 시설 %d개)
            - 수요 신호: %d/10점

            %s
            [반경 내 주요 경쟁사 (거리순 상위 5개)]
            %s

            [최근 폐업한 동일 업종 시설]
            %s

            위 데이터를 근거로 창업자에게 실질적인 입지 분석 보고서를 JSON으로 작성하세요.
            예산 범위를 고려해 초기 투자 전략과 손익분기점 관련 인사이트를 포함하세요.
            {
              "summary": "2-3문장. 이 입지의 핵심 특징과 창업 가능성 요약 (지역명, 점수, 등급 언급)",
              "market_analysis": "3-4문장. 시장 규모, 수요 현황, 상권 특성 분석. 구체적 수치 필수 포함",
              "competition_analysis": "3-4문장. 경쟁 강도, 주요 경쟁사 특징, 차별화 전략 방향",
              "budget_analysis": "2-3문장. %s 예산 기준 예상 초기 투자 구성(임대보증금·인테리어·기기), 손익분기점 추정",
              "risks": ["위험 요인 1 (수치 포함)", "위험 요인 2", "위험 요인 3"],
              "opportunities": ["기회 요인 1 (구체적)", "기회 요인 2", "기회 요인 3"],
              "recommendations": ["실행 전략 1 (구체적·실행 가능)", "전략 2", "전략 3"],
              "score_reasoning": "2문장. %d점을 받은 핵심 이유와 개선 포인트"
            }
            """.formatted(
            address != null ? address : "미지정",
            budgetContext,
            category, radiusM,
            score.total(), score.grade(),
            score.competitionScore(), score.competitorCount(),
            score.viabilityScore(), score.closureRate() * 100, score.closedCount(),
            score.publicPressureScore(), score.publicCount(),
            score.areaVitalityScore(), score.allActiveCount(),
            score.demandScore(),
            regionCtx,
            competitorList,
            closedList,
            budgetRange != null ? budgetRange : "미지정",
            score.total()
        );
    }

    private String fallbackJson(ScoreResult score, String category) {
        return """
            {
              "summary": "%s 입지 분석이 완료되었습니다. 총 %d점(%c등급)을 기록했습니다.",
              "market_analysis": "반경 내 %d개의 동일 업종이 운영 중이며, 폐업률은 %.1f%%입니다.",
              "competition_analysis": "현재 %d개의 경쟁 시설이 운영 중입니다.",
              "risks": ["경쟁 시설 과밀 위험", "공공시설과의 경쟁", "시장 포화 가능성"],
              "opportunities": ["차별화된 서비스 제공", "미충족 수요 공략", "상권 성장 가능성"],
              "recommendations": ["차별화 전략 수립", "초기 마케팅 강화", "고객층 세분화"],
              "score_reasoning": "데이터 기반 점수 산정이 완료되었습니다."
            }
            """.formatted(
            category, score.total(), score.grade(),
            score.competitorCount(), score.closureRate() * 100,
            score.competitorCount()
        );
    }
}
