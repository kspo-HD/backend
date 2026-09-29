package com.fitmap.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WebSearchService {

    private static final String TAVILY_URL = "https://api.tavily.com/search";

    @Value("${tavily.api-key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    public String search(String category, String sido) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }

        List<String> queries = List.of(
            sido + " " + category + " 창업 트렌드 2024",
            category + " 폐업 원인 성공 전략",
            category + " 창업 비용 절감"
        );

        StringBuilder results = new StringBuilder();
        for (String query : queries) {
            try {
                String snippet = searchOne(query);
                if (!snippet.isBlank()) {
                    results.append("· ").append(snippet).append("\n");
                }
            } catch (Exception e) {
                log.warn("웹 검색 실패 [{}]: {}", query, e.getMessage());
            }
        }

        if (results.isEmpty()) return "";
        return "\n[외부 시장 동향 참고]\n" + results;
    }

    @SuppressWarnings("unchecked")
    private String searchOne(String query) {
        Map<String, Object> body = Map.of(
            "api_key", apiKey,
            "query", query,
            "search_depth", "basic",
            "max_results", 2,
            "include_answer", true
        );

        Map<?, ?> response = restClient.post()
            .uri(TAVILY_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map.class);

        if (response == null) return "";

        String answer = (String) response.get("answer");
        if (answer != null && !answer.isBlank()) return answer;

        List<?> results = (List<?>) response.get("results");
        if (results == null || results.isEmpty()) return "";

        return results.stream()
            .limit(2)
            .map(r -> ((Map<?, ?>) r).get("content"))
            .filter(c -> c instanceof String)
            .map(c -> ((String) c).length() > 200 ? ((String) c).substring(0, 200) + "..." : (String) c)
            .collect(Collectors.joining(" / "));
    }
}
