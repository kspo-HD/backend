package com.fitmap.api.v1;

import com.fitmap.domain.facility.Facility;
import com.fitmap.repository.FacilityRepository;
import com.fitmap.service.FacilityService;
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
    private final FacilityService facilityService;

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
    @GetMapping("/map")
    public ResponseEntity<List<Map<String,Object>>> mapPoints(
        @RequestParam(required = false) String sido,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String isPublic
    ) {
        return ResponseEntity.ok(facilityService.getMapPoints(sido, category, isPublic));
    }

    /** 전체 시설 통계 — 대시보드용 */
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(facilityService.getStats());
    }

    /** 시설 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<Facility> detail(@PathVariable Long id) {
        return facilityRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
