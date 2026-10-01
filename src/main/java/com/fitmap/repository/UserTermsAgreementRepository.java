package com.fitmap.repository;

import com.fitmap.domain.terms.UserTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, Long> {
    List<UserTermsAgreement> findByUser_Id(Long userId);

    @Modifying
    @Query("DELETE FROM UserTermsAgreement u WHERE u.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);
}
