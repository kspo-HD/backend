package com.fitmap.api.v1;

import com.fitmap.domain.facility.Facility;
import com.fitmap.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityRepository facilityRepository;

    /** 반경 내 시설 목록 — 카카오맵 마커용 */
    @GetMapping("/nearby")
    public ResponseEntity<List<Facility>> nearby(
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestParam(defaultValue = "1000") int radius,
        @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(facilityRepository.findNearby(lat, lng, radius, category));
    }

    /** 지도용 경량 포인트 목록 — 히트맵·마커 */
    @Cacheable(value = "map-points", key = "#sido + ':' + #category + ':' + #isPublic")
    @GetMapping("/map")
    public ResponseEntity<List<Map<String,Object>>> mapPoints(
        @RequestParam(required = false) String sido,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String isPublic
    ) {
        List<Object[]> rows = facilityRepository.findForMap(sido, category, isPublic);
        List<Map<String,Object>> result = rows.stream().map(r -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("lat", r[0]);
            m.put("lng", r[1]);
            m.put("name", r[2]);
            m.put("category", r[3]);
            m.put("isPublic", r[4]);
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /** 전체 시설 통계 — 대시보드용 */
    @Cacheable("facility-stats")
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        List<Object[]> categoryRows = facilityRepository.countByCategory();
        List<Object[]> regionRows = facilityRepository.countBySido();
        List<Object[]> typeRows = facilityRepository.countByType();
        long total = facilityRepository.count();
        long active = facilityRepository.countActive();

        List<Map<String,Object>> categories = categoryRows.stream()
            .map(r -> Map.<String,Object>of("category", r[0], "count", r[1]))
            .toList();
        List<Map<String,Object>> regions = regionRows.stream()
            .map(r -> Map.<String,Object>of("sido", r[0], "count", r[1]))
            .toList();
        List<Map<String,Object>> types = typeRows.stream()
            .map(r -> Map.<String,Object>of("type", r[0], "count", r[1]))
            .toList();

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("active", active);
        result.put("categories", categories);
        result.put("regions", regions);
        result.put("types", types);
        return ResponseEntity.ok(result);
    }

    /** 시설 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<Facility> detail(@PathVariable Long id) {
        return facilityRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
