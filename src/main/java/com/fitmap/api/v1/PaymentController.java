package com.fitmap.api.v1;

import com.fitmap.domain.payment.Credit;
import com.fitmap.domain.payment.Payment;
import com.fitmap.domain.user.User;
import com.fitmap.repository.CreditRepository;
import com.fitmap.repository.PaymentRepository;
import com.fitmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Map<Integer, Integer> BUNDLE_PRICE = Map.of(1, 9900, 3, 24900, 5, 39000);

    private final PaymentRepository paymentRepository;
    private final CreditRepository creditRepository;
    private final UserRepository userRepository;

    public record PurchaseRequest(int bundleType) {}

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        List<Map<String, Object>> result = paymentRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("bundleType", p.getBundleType());
                m.put("totalCredits", p.getTotalCredits());
                m.put("amount", p.getAmount());
                m.put("status", p.getStatus());
                m.put("createdAt", p.getCreatedAt());
                return m;
            })
            .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> purchase(@RequestBody PurchaseRequest req, Authentication auth) {
        if (!BUNDLE_PRICE.containsKey(req.bundleType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid bundle type"));
        }

        User user = userRepository.findById(Long.parseLong(auth.getName())).orElseThrow();
        int amount = BUNDLE_PRICE.get(req.bundleType());

        Payment payment = Payment.builder()
            .user(user)
            .bundleType((short) req.bundleType())
            .totalCredits((short) req.bundleType())
            .amount(amount)
            .pgTxId("DUMMY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .pgProvider("dummy")
            .status("paid")
            .build();
        paymentRepository.save(payment);

        List<Credit> credits = new ArrayList<>();
        for (int i = 0; i < req.bundleType(); i++) {
            credits.add(Credit.builder().user(user).payment(payment).build());
        }
        creditRepository.saveAll(credits);

        return ResponseEntity.ok(Map.of(
            "paymentId", payment.getId(),
            "creditsAdded", req.bundleType(),
            "amount", amount
        ));
    }
}
