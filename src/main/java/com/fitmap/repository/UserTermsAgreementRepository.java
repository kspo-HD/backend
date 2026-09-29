package com.fitmap.repository;

import com.fitmap.domain.terms.UserTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, Long> {
    List<UserTermsAgreement> findByUser_Id(Long userId);
}
