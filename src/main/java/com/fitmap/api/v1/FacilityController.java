package com.fitmap.api.v1;

import com.fitmap.domain.facility.Facility;
import com.fitmap.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /** 시설 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<Facility> detail(@PathVariable Long id) {
        return facilityRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
