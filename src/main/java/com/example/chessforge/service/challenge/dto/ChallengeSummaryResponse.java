package com.example.chessforge.service.challenge.dto;

import com.example.chessforge.model.enums.challenge.ColorPreference;
import com.example.chessforge.model.enums.timeControl.TimeControl;

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