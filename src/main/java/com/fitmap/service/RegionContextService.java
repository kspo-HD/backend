package com.fitmap.service;

import com.fitmap.domain.region.RegionPopulation;
import com.fitmap.domain.region.RegionRental;
import com.fitmap.domain.region.RegionSingleHousehold;
import com.fitmap.repository.FacilityRepository;
import com.fitmap.repository.RegionPopulationRepository;
import com.fitmap.repository.RegionRentalRepository;
import com.fitmap.repository.RegionSingleHouseholdRepository;
import com.fitmap.repository.SubwayStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionContextService {

    private final FacilityRepository facilityRepository;
    private final RegionPopulationRepository populationRepository;
    private final RegionRentalRepository rentalRepository;
    private final SubwayStationRepository subwayRepository;
    private final RegionSingleHouseholdRepository singleHouseholdRepository;

    public String buildContext(String address, String category, double lat, double lng, int radiusM) {
        String sido    = extractPart(address, 0);
        String sigungu = extractPart(address, 1);

        StringBuilder sb = new StringBuilder();

        // 1. 시설 현황 (기존)
        if (sido != null && !sido.isBlank()) {
            long sidoCategoryActive = facilityRepository.countActiveBySidoAndCategory(sido, category);
            long sidoAllActive      = facilityRepository.countActiveBySido(sido);
            sb.append("""
                [%s 지역 현황]
                - %s 업종 정상운영 시설: %d개
                - %s 전체 스포츠·체육 시설: %d개
                """.formatted(sido, category, sidoCategoryActive, sido, sidoAllActive));
        }

        // 2. 인구 데이터
        if (sido != null && sigungu != null) {
            Optional<RegionPopulation> pop = populationRepository.findBySidoAndSigungu(sido, sigungu);
            pop.ifPresent(p -> sb.append("""
                [인구 현황 - %s %s]
                - 총 주민등록 인구: %,d명
                - 주요 소비층(20~49세): %,d명 (%.1f%%)
                """.formatted(
                sido, sigungu,
                p.getTotalPop(),
                p.getPop2049(),
                p.getTotalPop() > 0 ? (double) p.getPop2049() / p.getTotalPop() * 100 : 0
            )));
        }

        // 3. 임대료 데이터
        if (sido != null) {
            rentalRepository.findBySido(sido).ifPresent(r -> sb.append("""
                [임대료 현황 - %s]
                - 1층 평균 임대료: %.1f천원/㎡ (%s 기준)
                """.formatted(sido, r.getAvgRent1f(), r.getQuarter())));
        }

        // 4. 1인가구 현황
        if (sido != null) {
            singleHouseholdRepository.findBySido(sido).ifPresent(s -> {
                double ratio2049 = s.getTotalSingleHh() > 0
                    ? (double) s.getSingleHh2049() / s.getTotalSingleHh() * 100 : 0;
                sb.append("""
                    [1인가구 현황 - %s]
                    - 총 1인가구 수: %,d세대
                    - 20~49세 1인가구(핵심 피트니스 수요층): %,d세대 (전체 1인가구의 %.1f%%)
                    - 연령별: 20대 %,d / 30대 %,d / 40대 %,d
                    """.formatted(
                    sido,
                    s.getTotalSingleHh(),
                    s.getSingleHh2049(), ratio2049,
                    s.getSingleHh2029(), s.getSingleHh3039(), s.getSingleHh4049()
                ));
            });
        }

        // 5. 반경 내 지하철역
        List<Object[]> nearbySubway = subwayRepository.findNearby(lat, lng, radiusM);
        if (!nearbySubway.isEmpty()) {
            String stationList = nearbySubway.stream()
                .map(row -> "  · %s역(%s) %.0fm".formatted(row[0], row[1], ((Number) row[2]).doubleValue()))
                .collect(Collectors.joining("\n"));
            sb.append("""
                [반경 %dm 내 지하철역 - %d개]
                %s
                """.formatted(radiusM, nearbySubway.size(), stationList));
        } else {
            sb.append("[반경 %dm 내 지하철역 없음]\n".formatted(radiusM));
        }

        return sb.toString();
    }

    private String extractPart(String address, int index) {
        if (address == null) return null;
        String[] parts = address.trim().split("\\s+");
        return parts.length > index ? parts[index] : null;
    }
}
