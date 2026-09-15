package com.example.chessforge.service.tournament.dto;

import com.example.chessforge.model.enums.tournament.TournamentParticipantStatus;

public record TournamentParticipantResponse(
        String username,
        Integer rating,
        Double score,
        Integer seed,
        TournamentParticipantStatus status
) {
}