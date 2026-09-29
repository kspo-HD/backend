package com.fitmap.service;

import com.fitmap.domain.facility.Facility;
import com.fitmap.service.ScoreCalculator.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    // 옵셔널 - Tavily API 키 있으면 외부 검색, 없으면 GPT 내부 지식 활용
    @Autowired(required = false)
    private WebSearchService webSearchService;

    public String generateReport(String category, int radiusM, ScoreResult score, List<Facility> allNearby,
                                  String address, String budgetRange, String regionContext) {

        String webContext = "";
        if (webSearchService != null) {
            webContext = webSearchService.search(category, extractSido(address));
        }

        String prompt = buildPrompt(category, radiusM, score, allNearby, address, budgetRange, regionContext, webContext);

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
            당신은 국내 피트니스·스포츠 창업 전문 컨설턴트이자 데이터 분석가입니다.
            제공된 공공데이터(시설 현황·인구·임대료·지하철 접근성)를 기반으로 분석하되,
            당신이 보유한 한국 피트니스·헬스장·스포츠시설 창업에 관한 전문 지식도 적극 활용하세요.

            반드시 JSON 형식만 반환하고, 분석은 한국어로 작성합니다.

            작성 원칙:
            1. 제공된 공공데이터 수치를 반드시 인용하세요 ('반경 내 경쟁사 X개', '폐업률 Y%').
            2. 당신의 산업 지식을 활용해 일반적인 비용 벤치마크, 성공/실패 패턴, 트렌드를 보완하세요.
            3. 비용 절감 전략은 실제 절약 가능한 금액이나 비율을 구체적으로 제시하세요.
            4. 폐업 방지 전략은 한국 소규모 피트니스 시설의 실제 폐업 원인(회원 이탈, 고정비 부담, 경쟁 격화)을 근거로 경고 신호와 대응책을 제시하세요.
            5. 각 섹션은 충분히 상세하게 작성하세요 (summary 최소 3문장, 분석 섹션 최소 5문장).
            6. recommendations는 실제 6개월 내 실행 가능한 구체적 액션 플랜으로 작성하세요.
            """;
    }

    private String buildPrompt(String category, int radiusM, ScoreResult score, List<Facility> allNearby,
                               String address, String budgetRange, String regionContext, String webContext) {
        List<Facility> topCompetitors = allNearby.stream()
            .filter(f -> category.equals(f.getCategory()) && "정상운영".equals(f.getStatus()))
            .limit(7)
            .toList();

        String competitorList = topCompetitors.isEmpty()
            ? "- 반경 내 동일 업종 운영 시설 없음"
            : topCompetitors.stream()
                .map(f -> "- %s (%s, %s, %s)".formatted(
                    f.getName(),
                    f.getRoadAddr() != null ? f.getRoadAddr() : f.getSigungu(),
                    f.getAreaM2() != null ? f.getAreaM2() + "㎡" : "규모 미상",
                    f.getIsPublic() != null && f.getIsPublic() ? "공공시설" : "민간"
                ))
                .collect(Collectors.joining("\n"));

        String closedList = allNearby.stream()
            .filter(f -> category.equals(f.getCategory()) && !"정상운영".equals(f.getStatus()))
            .limit(5)
            .map(f -> "- %s (폐업%s)".formatted(
                f.getName(),
                f.getClosedAt() != null ? ", " + f.getClosedAt() : ""
            ))
            .collect(Collectors.joining("\n"));
        if (closedList.isBlank()) closedList = "- 없음";

        String budgetContext = (budgetRange != null && !budgetRange.isBlank())
            ? "희망 창업 예산: " + budgetRange
            : "희망 창업 예산: 미지정";

        String regionCtx = (regionContext != null && !regionContext.isBlank()) ? regionContext : "";
        String webCtx    = (webContext   != null && !webContext.isBlank())
            ? "\n[외부 시장 동향]\n" + webContext : "";

        return """
            [창업자 정보]
            창업 예정 지역: %s
            %s
            창업 예정 업종: %s
            분석 반경: %dm

            [입지 점수 (공공데이터 기반)]
            총점: %d점 / %c등급
            - 경쟁 강도: %d/30점 (동일 업종 운영 시설 %d개)
            - 시장 생존율: %d/25점 (폐업률 %.1f%%, 폐업 시설 %d개)
            - 공공시설 압박: %d/20점 (무료·공공 시설 %d개)
            - 상권 활성도: %d/15점 (전체 운영 시설 %d개)
            - 수요 신호: %d/10점

            %s
            %s
            [반경 내 주요 경쟁사 (거리순 상위 7개)]
            %s

            [최근 폐업한 동일 업종 시설]
            %s

            위 공공데이터와 귀하의 피트니스·스포츠 창업 전문 지식을 결합하여, 창업자에게 실질적으로 도움이 되는 상세 입지 분석 보고서를 작성하세요.

            각 필드 작성 지침:
            - summary: 3-4문장. 이 입지의 핵심 결론과 창업 적합성 (지역명·점수·등급 언급 필수)
            - market_analysis: 5-6문장. 시장 규모·수요 현황·상권 특성·인구 구조·임대 환경 분석 (데이터 수치 필수)
            - competition_analysis: 5-6문장. 경쟁 강도·주요 경쟁사 특징·공공시설 압박 수준·차별화 필요성
            - budget_analysis: 4-5문장. %s 예산 기준 항목별 투자 배분(임대보증금 30-40%%·인테리어 30%%·장비 20-25%%·운전자금 10%%), 월 고정비 추정, 손익분기 회원 수
            - cost_saving_tips: 최소 4개. 실제 절감 가능한 구체적 전략 (중고장비 활용, 소상공인 지원금, 공동 마케팅 등, 절감 금액/비율 포함)
            - survival_strategies: 최소 4개. 폐업 방지 핵심 전략 (회원 이탈 방지·현금흐름 관리·프로그램 다양화·지역 커뮤니티 구축, 목표 지표 포함)
            - risks: 최소 4개. 이 입지의 구체적 위험 요인 (데이터 기반 수치 포함)
            - opportunities: 최소 4개. 구체적 기회 요인 (인구·상권·트렌드 데이터 근거)
            - recommendations: 최소 5개. 개업 전·후 6개월 내 실행 가능한 액션 플랜 (시기 포함)
            - score_reasoning: 3-4문장. %d점의 핵심 이유, 등급 상향 조건, 재분석 권장 시점

            JSON 형식:
            {
              "summary": "...",
              "market_analysis": "...",
              "competition_analysis": "...",
              "budget_analysis": "...",
              "cost_saving_tips": ["...", "...", "...", "..."],
              "survival_strategies": ["...", "...", "...", "..."],
              "risks": ["...", "...", "...", "..."],
              "opportunities": ["...", "...", "...", "..."],
              "recommendations": ["...", "...", "...", "...", "..."],
              "score_reasoning": "..."
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
            webCtx,
            competitorList,
            closedList,
            budgetRange != null ? budgetRange : "미지정",
            score.total()
        );
    }

    private String extractSido(String address) {
        if (address == null) return "";
        String[] parts = address.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    private String fallbackJson(ScoreResult score, String category) {
        return """
            {
              "summary": "%s 입지 분석이 완료되었습니다. 총 %d점(%c등급)을 기록했습니다. 아래 세부 데이터를 참고하여 창업 전략을 수립하세요.",
              "market_analysis": "반경 내 %d개의 동일 업종이 운영 중이며, 폐업률은 %.1f%%입니다. AI 분석 생성 중 오류가 발생하여 상세 분석을 제공하지 못했습니다.",
              "competition_analysis": "현재 %d개의 경쟁 시설이 운영 중입니다. 차별화 전략 수립이 필요합니다.",
              "budget_analysis": "초기 투자 계획 수립 시 임대보증금 30-40%%, 인테리어 30%%, 장비 20-25%%, 운전자금 10%% 순으로 예산을 배분하는 것이 일반적입니다.",
              "cost_saving_tips": ["중고 운동기구 활용으로 장비 비용 30-40%% 절감", "소상공인 창업 지원금 신청", "인테리어 직영 시공 고려", "초기 운영 인력 최소화"],
              "survival_strategies": ["3개월치 운영 자금 예비금 확보", "월별 회원 이탈률 5%% 이하 유지 목표", "지역 커뮤니티 연계 프로그램 운영", "SNS 마케팅 채널 다변화"],
              "risks": ["경쟁 시설 과밀 위험", "공공시설 가격 경쟁", "회원 이탈 고정비 부담", "시장 포화 가능성"],
              "opportunities": ["차별화 서비스로 미충족 수요 공략", "인근 직장인 타겟 마케팅", "특화 프로그램 운영", "지역 스포츠 행사 연계"],
              "recommendations": ["창업 전 6개월 이상 시장 조사", "차별화 프로그램 개발", "초기 홍보 마케팅 강화", "고객층 세분화", "월별 손익 분석 체계 구축"],
              "score_reasoning": "데이터 기반 점수 산정이 완료되었습니다. AI 분석 생성 중 일시적 오류가 발생했습니다."
            }
            """.formatted(
            category, score.total(), score.grade(),
            score.competitorCount(), score.closureRate() * 100,
            score.competitorCount()
        );
    }
}
