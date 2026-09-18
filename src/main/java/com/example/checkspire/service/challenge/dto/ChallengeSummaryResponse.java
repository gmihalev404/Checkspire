package com.example.checkspire.service.challenge.dto;

import com.example.checkspire.model.enums.challenge.ColorPreference;
import com.example.checkspire.model.enums.timeControl.TimeControl;

import java.time.LocalDateTime;

public record ChallengeSummaryResponse(
        Long challengeId,
        String otherUsername,
        TimeControl timeControl,
        ColorPreference colorPreference,
        boolean rated,
        LocalDateTime expiresAt
) {
}