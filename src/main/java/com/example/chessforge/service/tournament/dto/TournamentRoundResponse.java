package com.example.chessforge.service.tournament.dto;

import com.example.chessforge.model.enums.tournament.TournamentRoundStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TournamentRoundResponse(

        Integer roundNumber,

        TournamentRoundStatus status,

        LocalDateTime scheduledAt,

        LocalDateTime startedAt,

        LocalDateTime completedAt,

        boolean current,

        List<TournamentMatchResponse> matches

) {
}