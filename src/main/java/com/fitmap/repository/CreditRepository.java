package com.fitmap.repository;

import com.fitmap.domain.payment.Credit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CreditRepository extends JpaRepository<Credit, UUID> {

    long countByUser_IdAndUsedAtIsNull(Long userId);

    @Modifying
    @Query("DELETE FROM Credit c WHERE c.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    @Query("""
        SELECT c FROM Credit c
        WHERE c.user.id = :userId AND c.usedAt IS NULL
        ORDER BY c.payment.createdAt ASC
        LIMIT 1
        """)
    Optional<Credit> findOldestUnused(@Param("userId") Long userId);
}
