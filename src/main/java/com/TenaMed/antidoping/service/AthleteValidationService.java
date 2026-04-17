package com.TenaMed.antidoping.service;

import com.TenaMed.antidoping.repository.AthleteProfileRepository;
import com.TenaMed.common.exception.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AthleteValidationService {

    private final AthleteProfileRepository athleteProfileRepository;

    public AthleteValidationService(AthleteProfileRepository athleteProfileRepository) {
        this.athleteProfileRepository = athleteProfileRepository;
    }

    @Transactional(readOnly = true)
    public void validateAthlete(UUID userId) {
        if (userId == null || !athleteProfileRepository.existsByUserId(userId)) {
            throw new ForbiddenException("Access denied: athlete profile is required");
        }
    }
}
