package com.fitmap.service;

import com.fitmap.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;

    @Cacheable(value = "map-points", key = "#sido + ':' + #category + ':' + #isPublic")
    public List<Map<String, Object>> getMapPoints(String sido, String category, String isPublic) {
        return facilityRepository.findForMap(sido, category, isPublic).stream()
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("lat", r[0]);
                m.put("lng", r[1]);
                m.put("name", r[2]);
                m.put("category", r[3]);
                m.put("isPublic", r[4]);
                return m;
            })
            .toList();
    }

    @Cacheable("facility-stats")
    public Map<String, Object> getStats() {
        List<Map<String, Object>> categories = facilityRepository.countByCategory().stream()
            .map(r -> Map.<String, Object>of("category", r[0], "count", r[1]))
            .toList();
        List<Map<String, Object>> regions = facilityRepository.countBySido().stream()
            .map(r -> Map.<String, Object>of("sido", r[0], "count", r[1]))
            .toList();
        List<Map<String, Object>> types = facilityRepository.countByType().stream()
            .map(r -> Map.<String, Object>of("type", r[0], "count", r[1]))
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", facilityRepository.count());
        result.put("active", facilityRepository.countActive());
        result.put("categories", categories);
        result.put("regions", regions);
        result.put("types", types);
        return result;
    }
}
