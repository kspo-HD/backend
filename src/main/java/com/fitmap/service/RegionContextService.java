package com.fitmap.service;

import com.fitmap.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegionContextService {

    private final FacilityRepository facilityRepository;

    public String buildContext(String address, String category) {
        String sido = extractSido(address);
        if (sido == null || sido.isBlank()) return "";

        long sidoCategoryActive = facilityRepository.countActiveBySidoAndCategory(sido, category);
        long sidoAllActive = facilityRepository.countActiveBySido(sido);

        return """
            [%s 지역 현황]
            - %s 업종 정상운영 시설: %d개
            - %s 전체 스포츠·체육 시설: %d개
            """.formatted(sido, category, sidoCategoryActive, sido, sidoAllActive);
    }

    private String extractSido(String address) {
        if (address == null) return null;
        String[] parts = address.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }
}
