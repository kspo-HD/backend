package com.fitmap.api.v1;

import com.fitmap.common.ApiResponse;
import com.fitmap.service.TermsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
public class TermsController {

    private final TermsService termsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(termsService.listActive()));
    }
}
