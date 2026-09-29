package com.fitmap.api.v1;

import com.fitmap.repository.CreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/credits")
public class CreditController {

    private final CreditRepository creditRepository;

    @GetMapping("/remaining")
    public ResponseEntity<?> remaining(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        long count = creditRepository.countByUser_IdAndUsedAtIsNull(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
