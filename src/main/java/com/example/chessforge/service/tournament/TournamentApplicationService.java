package com.example.chessforge.service.tournament;

import com.example.chessforge.controller.tournament.dto.TournamentCreateRequest;
import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.tournament.TournamentMatch;
import com.example.chessforge.model.entity.tournament.TournamentParticipant;
import com.example.chessforge.model.entity.tournament.TournamentRound;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.tournament.TournamentParticipantStatus;
import com.example.chessforge.model.enums.tournament.TournamentStatus;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.RatingService;
import com.example.chessforge.service.tournament.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentApplicationService {

    private final TournamentService tournamentService;

    private final RatingService ratingService;

    private final GameService gameService;


    // =========================================================
    // TOURNAMENT LIST
    // =========================================================

    public List<TournamentSummaryResponse> getTournaments() {

        return tournamentService
                .getAllTournaments()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Tournament::getUpdatedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .map(
                        this::toSummary
                )
                .toList();
    }


    private TournamentSummaryResponse toSummary(
            Tournament tournament
    ) {

        return new TournamentSummaryResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getCreator()
                        .getUsername(),
                tournament.getFormat(),
                tournament.getTimeControl(),
                tournament.isRated(),
                tournament.getStatus(),
                tournament.getMaxPlayers(),
                tournament.getStartsAt(),
                tournament.getUpdatedAt()
        );
    }


    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public Long createTournament(
            User creator,
            TournamentCreateRequest request
    ) {

        Tournament tournament =
                tournamentService.createTournament(
                        request.getName(),
                        creator,
                        request.getFormat(),
                        request.getTimeControl(),
                        request.isRated(),
                        request.getMaxPlayers(),
                        request.getStartsAt(),
                        request.getByePoints(),
                        request.getTieBreaks(),
                        request.isArmageddonForFirstPlaceTie(),
                        request.getNumberOfRounds()
                );

        return tournament.getId();
    }


    // =========================================================
    // DETAILS
    // =========================================================

    public TournamentDetailsResponse getTournamentDetails(
            Long tournamentId,
            User viewer
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );


        List<TournamentParticipant> participants =
                tournamentService
                        .getParticipants(
                                tournament
                        );


        // =====================================================
        // PARTICIPANTS
        // =====================================================

        List<TournamentParticipantResponse> participantResponses =
                participants
                        .stream()
                        .filter(participant ->
                                participant.getStatus()
                                        != TournamentParticipantStatus.WITHDRAWN
                        )
                        .map(participant ->
                                toParticipantResponse(
                                        participant,
                                        tournament
                                )
                        )
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                TournamentParticipantResponse::rating
                                        )
                                        .reversed()
                                        .thenComparing(
                                                TournamentParticipantResponse::username,
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                        )
                        .toList();


        // =====================================================
        // CURRENT ROUND MATCHES
        // =====================================================

        List<TournamentMatchResponse> matchResponses;

        if (
                tournament.getCurrentRound() == null
                        ||
                        tournament.getCurrentRound() <= 0
        ) {

            matchResponses =
                    List.of();

        } else {

            matchResponses =
                    tournamentService
                            .getRoundMatches(
                                    tournament,
                                    tournament.getCurrentRound()
                            )
                            .stream()
                            .map(match ->
                                    toMatchResponse(
                                            match,
                                            viewer
                                    )
                            )
                            .toList();
        }

        List<TournamentRoundResponse> roundResponses =
                tournamentService
                        .getRounds(
                                tournament
                        )
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        TournamentRound::getRoundNumber
                                ).reversed()
                        )
                        .map(round ->
                                toRoundResponse(
                                        round,
                                        tournament,
                                        viewer
                                )
                        )
                        .toList();

        // =====================================================
        // STANDINGS
        // =====================================================

        List<TournamentParticipant> standingParticipants;

        if (
                tournament.getStatus()
                        == TournamentStatus.FINISHED
        ) {

            standingParticipants =
                    participants
                            .stream()
                            .filter(participant ->
                                    participant.getFinalRank()
                                            != null
                            )
                            .sorted(
                                    Comparator.comparing(
                                            TournamentParticipant::getFinalRank
                                    )
                            )
                            .toList();

        } else if (
                tournament.getStatus()
                        == TournamentStatus.IN_PROGRESS
        ) {

            standingParticipants =
                    tournamentService
                            .getStandings(
                                    tournament
                            )
                            .stream()
                            .filter(participant ->
                                    participant.getStatus()
                                            != TournamentParticipantStatus.WITHDRAWN
                            )
                            .toList();

        } else {

            standingParticipants =
                    List.of();
        }


        List<TournamentParticipantResponse> standingResponses =
                standingParticipants
                        .stream()
                        .map(participant ->
                                toParticipantResponse(
                                        participant,
                                        tournament
                                )
                        )
                        .toList();


        // =====================================================
        // VIEWER STATE
        // =====================================================

        boolean viewerJoined =
                participants
                        .stream()
                        .anyMatch(participant ->
                                participant
                                        .getUser()
                                        .getId()
                                        .equals(
                                                viewer.getId()
                                        )
                                        &&
                                        participant.getStatus()
                                                == TournamentParticipantStatus.ACTIVE
                        );


        boolean viewerCreator =
                tournament
                        .getCreator()
                        .getId()
                        .equals(
                                viewer.getId()
                        );


        // =====================================================
        // RESPONSE
        // =====================================================

        return new TournamentDetailsResponse(
                tournament.getId(),
                tournament.getName(),
                tournament
                        .getCreator()
                        .getUsername(),
                tournament.getFormat(),
                tournament.getTimeControl(),
                tournament.isRated(),
                tournament.getStatus(),
                tournament.getMaxPlayers(),
                tournament.getStartsAt(),
                tournament.getByePoints(),
                tournament.getTieBreaks(),
                tournament.isArmageddonForFirstPlaceTie(),
                tournament.getNumberOfRounds(),
                tournament.getCurrentRound(),
                matchResponses,
                participantResponses,
                standingResponses,
                viewerJoined,
                viewerCreator,
                roundResponses
        );
    }


    // =========================================================
    // PARTICIPANT MAPPING
    // =========================================================

    private TournamentParticipantResponse toParticipantResponse(
            TournamentParticipant participant,
            Tournament tournament
    ) {

        int rating =
                participant.getRatingAtStart() != null
                        ? participant.getRatingAtStart()
                        : ratingService.getRating(
                        participant.getUser(),
                        tournament
                                .getTimeControl()
                                .getType()
                );


        return new TournamentParticipantResponse(
                participant
                        .getUser()
                        .getUsername(),
                rating,
                participant.getScore(),
                participant.getSeed(),
                participant.getFinalRank(),
                participant.getStatus()
        );
    }


    // =========================================================
    // MATCH MAPPING
    // =========================================================

    private TournamentMatchResponse toMatchResponse(
            TournamentMatch match,
            User viewer
    ) {

        Long gameId =
                gameService
                        .getLatestGameForTournamentMatch(
                                match
                        )
                        .map(
                                Game::getId
                        )
                        .orElse(
                                null
                        );


        boolean viewerIsWhite =
                match
                        .getWhiteParticipant()
                        .getUser()
                        .getId()
                        .equals(
                                viewer.getId()
                        );


        boolean viewerIsBlack =
                match.getBlackParticipant() != null
                        &&
                        match
                                .getBlackParticipant()
                                .getUser()
                                .getId()
                                .equals(
                                        viewer.getId()
                                );


        boolean viewerCanOpen =
                viewerIsWhite
                        ||
                        viewerIsBlack;


        return new TournamentMatchResponse(
                match.getId(),
                gameId,
                match.getRoundNumber(),
                match.getBoardNumber(),
                match
                        .getWhiteParticipant()
                        .getUser()
                        .getUsername(),
                match.getBlackParticipant() != null
                        ? match
                        .getBlackParticipant()
                        .getUser()
                        .getUsername()
                        : null,
                match.getStatus(),
                match.getType(),
                match.getWhiteScore(),
                match.getBlackScore(),
                viewerCanOpen
        );
    }


    // =========================================================
    // JOIN
    // =========================================================

    @Transactional
    public void joinTournament(
            Long tournamentId,
            User user
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );


        tournamentService.joinTournament(
                tournament,
                user
        );
    }

    // =========================================================
    // WITHDRAW
    // =========================================================

    @Transactional
    public void withdrawFromTournament(
            Long tournamentId,
            User user
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );


        tournamentService.withdraw(
                tournament,
                user
        );
    }


    // =========================================================
    // START
    // =========================================================

    @Transactional
    public void startTournament(
            Long tournamentId,
            User requester
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );


        tournamentService.startTournament(
                tournament,
                requester
        );


        gameService.startGamesForCurrentRound(
                tournament
        );
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @Transactional
    public void cancelTournament(
            Long tournamentId,
            User requester
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );


        tournamentService.cancelTournament(
                tournament,
                requester
        );
    }

    private TournamentRoundResponse toRoundResponse(
            TournamentRound round,
            Tournament tournament,
            User viewer
    ) {

        List<TournamentMatchResponse> matches =
                tournamentService
                        .getRoundMatches(
                                tournament,
                                round.getRoundNumber()
                        )
                        .stream()
                        .map(match ->
                                toMatchResponse(
                                        match,
                                        viewer
                                )
                        )
                        .toList();


        boolean current =
                tournament.getStatus()
                        == TournamentStatus.IN_PROGRESS
                        &&
                        tournament.getCurrentRound() != null
                        &&
                        tournament.getCurrentRound()
                                .equals(
                                        round.getRoundNumber()
                                );


        return new TournamentRoundResponse(
                round.getRoundNumber(),
                round.getStatus(),
                round.getScheduledAt(),
                round.getStartedAt(),
                round.getCompletedAt(),
                current,
                matches
        );
    }
}