package com.fitmap.api.v1;

import com.fitmap.common.ApiResponse;
import com.fitmap.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/credits")
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/remaining")
    public ResponseEntity<ApiResponse<Map<String, Long>>> remaining(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        long count = creditService.getRemainingCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> usage(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(creditService.getUsageHistory(userId)));
    }
}
