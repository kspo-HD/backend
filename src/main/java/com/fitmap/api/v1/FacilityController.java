package com.fitmap.api.v1;

import com.fitmap.domain.facility.Facility;
import com.fitmap.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
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

    /** 전체 시설 통계 — 대시보드용 */
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        List<Object[]> categoryRows = facilityRepository.countByCategory();
        List<Object[]> regionRows = facilityRepository.countBySido();
        long total = facilityRepository.count();

        List<Map<String,Object>> categories = categoryRows.stream()
            .map(r -> Map.<String,Object>of("category", r[0], "count", r[1]))
            .toList();
        List<Map<String,Object>> regions = regionRows.stream()
            .map(r -> Map.<String,Object>of("sido", r[0], "count", r[1]))
            .toList();

        return ResponseEntity.ok(Map.of(
            "total", total,
            "categories", categories,
            "regions", regions
        ));
    }

    /** 시설 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<Facility> detail(@PathVariable Long id) {
        return facilityRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
