package com.fitmap.service;

import com.fitmap.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsRepository termsRepository;

    public List<Map<String, Object>> listActive() {
        return termsRepository.findByIsActiveTrueOrderById().stream()
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
    }
}
