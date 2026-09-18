package com.example.checkspire.service.tournament.dto;

import com.example.checkspire.model.enums.timeControl.TimeControl;
import com.example.checkspire.model.enums.tournament.TournamentFormat;
import com.example.checkspire.model.enums.tournament.TournamentStatus;

import java.time.LocalDateTime;

public record TournamentSummaryResponse(
        Long id,
        String name,
        String creatorUsername,
        TournamentFormat format,
        TimeControl timeControl,
        boolean rated,
        TournamentStatus status,
        Integer maxPlayers,
        LocalDateTime startsAt,
        LocalDateTime updatedAt
) {
}