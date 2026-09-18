package com.example.checkspire.service.tournament.dto;

import com.example.checkspire.model.enums.timeControl.TimeControl;
import com.example.checkspire.model.enums.tournament.TieBreakType;
import com.example.checkspire.model.enums.tournament.TournamentFormat;
import com.example.checkspire.model.enums.tournament.TournamentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TournamentDetailsResponse(
        Long id,
        String name,
        String creatorUsername,
        TournamentFormat format,
        TimeControl timeControl,
        boolean rated,
        TournamentStatus status,
        Integer maxPlayers,
        LocalDateTime startsAt,
        double byePoints,
        List<TieBreakType> tieBreaks,
        boolean armageddonForFirstPlaceTie,
        Integer numberOfRounds,
        Integer currentRound,
        List<TournamentMatchResponse> matches,
        List<TournamentParticipantResponse> participants,
        List<TournamentParticipantResponse> standings,
        boolean viewerJoined,
        boolean viewerCreator,
        List<TournamentRoundResponse> rounds,
        boolean automaticStart,
        Integer roundBreakMinutes
) {
}