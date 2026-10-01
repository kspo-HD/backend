package com.fitmap.service;

import com.fitmap.repository.CreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;

    public long getRemainingCount(Long userId) {
        return creditRepository.countByUser_IdAndUsedAtIsNull(userId);
    }
}
