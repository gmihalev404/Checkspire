package com.example.checkspire.service.tournament.dto;

import com.example.checkspire.model.enums.tournament.TournamentParticipantStatus;

public record TournamentParticipantResponse(
        String username,
        Integer rating,
        Double score,
        Integer seed,
        Integer finalRank,
        TournamentParticipantStatus status
) {
}