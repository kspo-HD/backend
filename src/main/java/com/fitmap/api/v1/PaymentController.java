package com.fitmap.api.v1;

import com.fitmap.common.ApiResponse;
import com.fitmap.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public record PurchaseRequest(int bundleType) {}

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(paymentService.list(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> purchase(@RequestBody PurchaseRequest req, Authentication auth) {
        if (!PaymentService.BUNDLE_PRICE.containsKey(req.bundleType())) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("invalid bundle type"));
        }
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(paymentService.purchase(req.bundleType(), userId)));
    }
}
