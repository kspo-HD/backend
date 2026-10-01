package com.fitmap.repository;

import com.fitmap.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByUser_IdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("DELETE FROM Payment p WHERE p.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);
}
