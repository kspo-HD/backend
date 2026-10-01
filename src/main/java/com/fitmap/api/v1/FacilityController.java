package com.fitmap.api.v1;

import com.fitmap.common.ApiResponse;
import com.fitmap.domain.facility.Facility;
import com.fitmap.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<Facility>>> nearby(
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestParam(defaultValue = "1000") int radius,
        @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(ApiResponse.ok(facilityService.getNearby(lat, lng, radius, category)));
    }

    @GetMapping("/map")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> mapPoints(
        @RequestParam(required = false) String sido,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String isPublic
    ) {
        return ResponseEntity.ok(ApiResponse.ok(facilityService.getMapPoints(sido, category, isPublic)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(facilityService.getStats()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Facility>> detail(@PathVariable Long id) {
        Facility facility = facilityService.getById(id)
            .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + id));
        return ResponseEntity.ok(ApiResponse.ok(facility));
    }
}
