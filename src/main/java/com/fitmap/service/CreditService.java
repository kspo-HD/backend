package com.fitmap.service;

import com.fitmap.repository.CreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;

    public long getRemainingCount(Long userId) {
        return creditRepository.countByUser_IdAndUsedAtIsNull(userId);
    }

    public List<Map<String, Object>> getUsageHistory(Long userId) {
        return creditRepository.findUsedByUserId(userId).stream()
            .map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("usedAt", c.getUsedAt());
                if (c.getReport() != null) {
                    m.put("reportId", c.getReport().getId());
                    m.put("score", c.getReport().getScore());
                    m.put("grade", String.valueOf(c.getReport().getGrade()));
                    var analysis = c.getReport().getAnalysis();
                    if (analysis != null) {
                        m.put("category", analysis.getCategory());
                        m.put("address", analysis.getAddress());
                        m.put("radiusM", analysis.getRadiusM());
                    }
                }
                return m;
            })
            .toList();
    }
}
