package com.fitmap.service;

import com.fitmap.domain.payment.Credit;
import com.fitmap.domain.payment.Payment;
import com.fitmap.domain.user.User;
import com.fitmap.repository.CreditRepository;
import com.fitmap.repository.PaymentRepository;
import com.fitmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    public static final Map<Integer, Integer> BUNDLE_PRICE = Map.of(1, 9900, 3, 24900, 5, 39000);

    private final PaymentRepository paymentRepository;
    private final CreditRepository creditRepository;
    private final UserRepository userRepository;

    public List<Map<String, Object>> list(Long userId) {
        return paymentRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
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
    }

    @Transactional
    public Map<String, Object> purchase(int bundleType, Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        int amount = BUNDLE_PRICE.get(bundleType);

        Payment payment = Payment.builder()
            .user(user)
            .bundleType((short) bundleType)
            .totalCredits((short) bundleType)
            .amount(amount)
            .pgTxId("DUMMY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .pgProvider("dummy")
            .status("paid")
            .build();
        paymentRepository.save(payment);

        List<Credit> credits = new ArrayList<>();
        for (int i = 0; i < bundleType; i++) {
            credits.add(Credit.builder().user(user).payment(payment).build());
        }
        creditRepository.saveAll(credits);

        return Map.of(
            "paymentId", payment.getId(),
            "creditsAdded", bundleType,
            "amount", amount
        );
    }
}
