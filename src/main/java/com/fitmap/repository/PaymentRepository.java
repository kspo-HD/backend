package com.fitmap.repository;

import com.fitmap.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByUser_IdOrderByCreatedAtDesc(Long userId);

    void deleteByUser_Id(Long userId);
}
