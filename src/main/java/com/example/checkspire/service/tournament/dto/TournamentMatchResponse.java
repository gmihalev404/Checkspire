package com.example.checkspire.service.tournament.dto;

import com.example.checkspire.model.enums.tournament.TournamentMatchStatus;
import com.example.checkspire.model.enums.tournament.TournamentMatchType;

public record TournamentMatchResponse(
        Long id,
        Long gameId,
        Integer roundNumber,
        Integer boardNumber,
        String whiteUsername,
        String blackUsername,
        TournamentMatchStatus status,
        TournamentMatchType type,
        Double whiteScore,
        Double blackScore,
        boolean viewerCanOpen
) {
}