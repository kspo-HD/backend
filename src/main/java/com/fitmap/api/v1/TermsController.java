package com.fitmap.api.v1;

import com.fitmap.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
public class TermsController {

    private final TermsRepository termsRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        List<Map<String, Object>> result = termsRepository.findByIsActiveTrueOrderById().stream()
            .map(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.getId());
                m.put("title", t.getTitle());
                m.put("content", t.getContent());
                m.put("version", t.getVersion());
                m.put("isRequired", t.getIsRequired());
                return m;
            })
            .toList();
        return ResponseEntity.ok(result);
    }
}
