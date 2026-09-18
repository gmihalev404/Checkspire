package com.example.checkspire.service.tournament;

import com.example.checkspire.model.entity.game.Game;
import com.example.checkspire.model.entity.tournament.Tournament;
import com.example.checkspire.model.entity.tournament.TournamentMatch;
import com.example.checkspire.model.entity.tournament.TournamentParticipant;
import com.example.checkspire.model.entity.tournament.TournamentRound;
import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.model.enums.game.GameStatus;
import com.example.checkspire.model.enums.timeControl.TimeControl;
import com.example.checkspire.model.enums.timeControl.TimeControlType;
import com.example.checkspire.model.enums.tournament.*;
import com.example.checkspire.model.enums.user.UserStatus;
import com.example.checkspire.repository.tournament.TournamentMatchRepository;
import com.example.checkspire.repository.tournament.TournamentParticipantRepository;
import com.example.checkspire.repository.tournament.TournamentRepository;
import com.example.checkspire.repository.tournament.TournamentRoundRepository;
import com.example.checkspire.service.game.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentMatchRepository matchRepository;

    private final TournamentPairingService pairingService;
    private final TournamentLeaderboardService leaderboardService;
    private final RatingService ratingService;
    private final TournamentRoundRepository roundRepository;

    // =========================================================
    // CREATE TOURNAMENT
    // =========================================================

    @Transactional
    public Tournament createTournament(
            String name,
            User creator,
            TournamentFormat format,
            TimeControl timeControl,
            boolean rated,
            Integer maxPlayers,
            LocalDateTime startsAt,
            double byePoints,
            List<TieBreakType> tieBreaks,
            boolean armageddonForFirstPlaceTie,
            Integer numberOfRounds
    ) {

        if (creator.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active users can create tournaments."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Tournament name cannot be empty."
            );
        }

        if (format == null) {
            throw new IllegalArgumentException(
                    "Tournament format cannot be null."
            );
        }

        if (timeControl == null) {
            throw new IllegalArgumentException(
                    "Time control cannot be null."
            );
        }

        if (maxPlayers != null && maxPlayers < 2) {
            throw new IllegalArgumentException(
                    "A tournament must allow at least two players."
            );
        }

        if (byePoints < 0.0 || byePoints > 1.0) {
            throw new IllegalArgumentException(
                    "Bye points must be between 0 and 1."
            );
        }

        if (format == TournamentFormat.SWISS
                && (numberOfRounds == null
                || numberOfRounds < 1)) {

            throw new IllegalArgumentException(
                    "A Swiss tournament must have at least one round."
            );
        }

        int storedNumberOfRounds =
                format == TournamentFormat.SWISS
                        ? numberOfRounds
                        : 0;

        Tournament tournament = Tournament.builder()
                .name(name)
                .creator(creator)
                .format(format)
                .timeControl(timeControl)
                .rated(rated)
                .maxPlayers(maxPlayers)
                .startsAt(startsAt)
                .byePoints(byePoints)
                .tieBreaks(
                        tieBreaks == null
                                ? new ArrayList<>()
                                : new ArrayList<>(tieBreaks)
                )
                .armageddonForFirstPlaceTie(
                        armageddonForFirstPlaceTie
                )
                .status(
                        TournamentStatus.REGISTRATION
                )
                .numberOfRounds(
                        storedNumberOfRounds
                )
                .currentRound(0)
                .build();

        return tournamentRepository.save(
                tournament
        );
    }

    @Transactional
    public Tournament createTournament(
            String name,
            User creator,
            TournamentFormat format,
            TimeControl timeControl,
            boolean rated,
            Integer maxPlayers,
            LocalDateTime startsAt,
            boolean automaticStart,
            Integer roundBreakMinutes,
            double byePoints,
            List<TieBreakType> tieBreaks,
            boolean armageddonForFirstPlaceTie,
            Integer numberOfRounds
    ) {

        if (roundBreakMinutes == null
                || roundBreakMinutes < 0) {

            throw new IllegalArgumentException(
                    "Round break minutes cannot be negative."
            );
        }

        Tournament tournament =
                createTournament(
                        name,
                        creator,
                        format,
                        timeControl,
                        rated,
                        maxPlayers,
                        startsAt,
                        byePoints,
                        tieBreaks,
                        armageddonForFirstPlaceTie,
                        numberOfRounds
                );

        tournament.setAutomaticStart(
                automaticStart
        );

        tournament.setRoundBreakMinutes(
                roundBreakMinutes
        );

        return tournament;
    }

    // =========================================================
    // JOIN TOURNAMENT
    // =========================================================

    @Transactional
    public TournamentParticipant joinTournament(
            Tournament tournament,
            User user
    ) {

        if (tournament.getStatus()
                != TournamentStatus.REGISTRATION) {

            throw new IllegalStateException(
                    "Tournament registration is closed."
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active users can join tournaments."
            );
        }

        var existingParticipant =
                participantRepository
                        .findByTournamentAndUser(
                                tournament,
                                user
                        );

        if (existingParticipant.isPresent()) {

            TournamentParticipant participant =
                    existingParticipant.get();

            if (
                    participant.getStatus()
                            != TournamentParticipantStatus.WITHDRAWN
            ) {

                throw new IllegalStateException(
                        "User is already registered for this tournament."
                );
            }

            if (isFull(tournament)) {

                throw new IllegalStateException(
                        "Tournament is full."
                );
            }

            participant.setStatus(
                    TournamentParticipantStatus.ACTIVE
            );

            return participant;
        }

        if (isFull(tournament)) {
            throw new IllegalStateException(
                    "Tournament is full."
            );
        }

        TournamentParticipant participant =
                TournamentParticipant.builder()
                        .tournament(
                                tournament
                        )
                        .user(
                                user
                        )
                        .score(
                                0.0
                        )
                        .status(
                                TournamentParticipantStatus.ACTIVE
                        )
                        .build();

        tournament.touch();

        return participantRepository.save(
                participant
        );
    }

    // =========================================================
    // WITHDRAW BEFORE START
    // =========================================================

    @Transactional
    public void withdraw(
            Tournament tournament,
            User user
    ) {

        if (tournament.getStatus()
                != TournamentStatus.REGISTRATION) {

            throw new IllegalStateException(
                    "Players can only withdraw during registration."
            );
        }

        TournamentParticipant participant =
                getParticipant(
                        tournament,
                        user
                );

        if (participant.getStatus()
                != TournamentParticipantStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Participant is not active."
            );
        }

        participant.setStatus(
                TournamentParticipantStatus.WITHDRAWN
        );

        tournament.touch();
    }

    // =========================================================
    // FORFEIT AFTER START
    // =========================================================

    @Transactional
    public void forfeit(
            Tournament tournament,
            User user
    ) {

        if (tournament.getStatus()
                != TournamentStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "A participant can only forfeit an active tournament."
            );
        }

        TournamentParticipant participant =
                getParticipant(
                        tournament,
                        user
                );

        List<TournamentMatch> currentRoundMatches =
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                tournament.getCurrentRound()
                        );

        boolean activeParticipant =
                participant.getStatus()
                        == TournamentParticipantStatus.ACTIVE;

        boolean thirdPlaceParticipant =
                tournament.getFormat()
                        == TournamentFormat.SINGLE_ELIMINATION
                        &&
                        participant.getStatus()
                                == TournamentParticipantStatus.ELIMINATED
                        &&
                        isCurrentThirdPlaceParticipant(
                                participant,
                                currentRoundMatches
                        );

        if (!activeParticipant
                && !thirdPlaceParticipant) {

            throw new IllegalStateException(
                    "Participant cannot forfeit from the current tournament state."
            );
        }

        participant.setStatus(
                TournamentParticipantStatus.FORFEITED
        );

        boolean hasUnfinishedCurrentMatch =
                currentRoundMatches
                        .stream()
                        .filter(match ->
                                match.getStatus()
                                        != TournamentMatchStatus.COMPLETED
                        )
                        .anyMatch(match ->
                                sameParticipant(
                                        match.getWhiteParticipant(),
                                        participant
                                )
                                        ||
                                        sameParticipant(
                                                match.getBlackParticipant(),
                                                participant
                                        )
                        );


        if (hasUnfinishedCurrentMatch) {

            resolveAutomaticResults(
                    tournament,
                    currentRoundMatches
            );

            handleRoundCompletion(
                    tournament
            );
        }


        tournament.touch();
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @Transactional
    public void cancelTournament(
            Tournament tournament,
            User requester
    ) {

        validateCreator(
                tournament,
                requester
        );

        if (tournament.getStatus()
                == TournamentStatus.FINISHED) {

            throw new IllegalStateException(
                    "A finished tournament cannot be cancelled."
            );
        }

        if (tournament.getStatus()
                == TournamentStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Tournament is already cancelled."
            );
        }

        tournament.setStatus(
                TournamentStatus.CANCELLED
        );

        tournament.touch();
    }

    // =========================================================
    // START TOURNAMENT
    // =========================================================

    @Transactional
    public List<TournamentMatch> startTournament(
            Tournament tournament,
            User requester
    ) {

        validateCreator(
                tournament,
                requester
        );

        return startTournamentInternal(
                tournament
        );
    }


    @Transactional
    public Tournament startTournamentAutomatically(
            Long tournamentId
    ) {

        Tournament tournament =
                tournamentRepository
                        .findById(
                                tournamentId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Tournament not found."
                                )
                        );


        if (!tournament.isAutomaticStart()) {

            throw new IllegalStateException(
                    "Tournament is not configured for automatic start."
            );
        }


        if (tournament.getStartsAt() == null) {

            throw new IllegalStateException(
                    "Tournament has no scheduled start time."
            );
        }


        if (tournament.getStartsAt()
                .isAfter(
                        LocalDateTime.now()
                )) {

            throw new IllegalStateException(
                    "Tournament is not due yet."
            );
        }


        startTournamentInternal(
                tournament
        );

        return tournament;
    }


    private List<TournamentMatch> startTournamentInternal(
            Tournament tournament
    ) {

        if (tournament.getStatus()
                != TournamentStatus.REGISTRATION) {

            throw new IllegalStateException(
                    "Only a tournament in registration can be started."
            );
        }


        List<TournamentParticipant> participants =
                getActiveParticipants(
                        tournament
                );


        validateTournamentCanStart(
                tournament,
                participants
        );


        assignSeeds(
                tournament,
                participants
        );


        List<TournamentMatch> matches =
                pairingService
                        .createInitialPairings(
                                tournament,
                                participants
                        );


        matchRepository.saveAll(
                matches
        );


        tournament.setStatus(
                TournamentStatus.IN_PROGRESS
        );

        tournament.setCurrentRound(
                1
        );


        startRound(
                tournament,
                1,
                tournament.getStartsAt()
        );


        resolveAutomaticResults(
                tournament,
                matches.stream()
                        .filter(match ->
                                match.getRoundNumber() == 1
                        )
                        .toList()
        );


        tournament.touch();

        return matches;
    }

    // =========================================================
    // NEXT ROUND
    // =========================================================

    @Transactional
    public List<TournamentMatch> scheduleNextRound(
            Tournament tournament
    ) {

        if (tournament.getStatus()
                != TournamentStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Only an active tournament can advance to the next round."
            );
        }

        if (tournament.getCurrentRound() == null) {

            throw new IllegalStateException(
                    "Tournament has no active round."
            );
        }

        // =====================================================
        // SWISS - LAST ROUND CHECK
        // =====================================================

        if (tournament.getFormat()
                == TournamentFormat.SWISS
                &&
                tournament.getCurrentRound()
                        >= tournament.getNumberOfRounds()) {

            throw new IllegalStateException(
                    "The Swiss tournament has no more rounds."
            );
        }

        // =====================================================
        // CURRENT ROUND MUST BE FINISHED
        // =====================================================

        validateCurrentRoundCompleted(
                tournament
        );

        int nextRoundNumber =
                tournament.getCurrentRound() + 1;

        List<TournamentMatch> matches;

        // =====================================================
        // ROUND ROBIN
        // =====================================================

        if (tournament.getFormat()
                == TournamentFormat.ROUND_ROBIN) {

            /*
             * The complete round-robin schedule
             * already exists.
             */
            matches =
                    matchRepository
                            .findByTournamentAndRoundNumberOrderByBoardNumber(
                                    tournament,
                                    nextRoundNumber
                            );

            if (matches.isEmpty()) {

                throw new IllegalStateException(
                        "There are no more rounds."
                );
            }
        }

        // =====================================================
        // SINGLE ELIMINATION / SWISS
        // =====================================================

        else {

            List<TournamentParticipant> participants =
                    participantRepository
                            .findByTournament(
                                    tournament
                            );

            /*
             * Swiss uses the complete history for:
             * - rematches
             * - colors
             * - BYEs
             *
             * Single elimination selects the
             * previous round internally.
             */
            List<TournamentMatch> matchHistory =
                    matchRepository
                            .findByTournament(
                                    tournament
                            );

            matches =
                    pairingService
                            .createNextRound(
                                    tournament,
                                    participants,
                                    matchHistory,
                                    nextRoundNumber
                            );

            matchRepository.saveAll(
                    matches
            );
        }

        // =====================================================
        // ACTIVATE ROUND
        // =====================================================

        LocalDateTime scheduledAt =
                LocalDateTime.now()
                        .plusMinutes(
                                tournament.getRoundBreakMinutes()
                        );

        scheduleRound(
                tournament,
                nextRoundNumber,
                scheduledAt
        );

        tournament.touch();

        return matches;
    }

    // =========================================================
    // ACTIVATE SCHEDULED ROUND
    // =========================================================

    @Transactional
    public Tournament activateScheduledRound(
            Long roundId
    ) {

        TournamentRound round =
                roundRepository
                        .findById(
                                roundId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Tournament round not found."
                                )
                        );


        if (round.getScheduledAt() == null) {

            throw new IllegalStateException(
                    "Scheduled round has no start time."
            );
        }


        if (round.getScheduledAt()
                .isAfter(
                        LocalDateTime.now()
                )) {

            throw new IllegalStateException(
                    "Tournament round is not due yet."
            );
        }


        return activateRound(
                round
        );
    }

    @Transactional
    public Tournament startScheduledRoundNow(
            Tournament tournament,
            Integer roundNumber,
            User requester
    ) {

        validateCreator(
                tournament,
                requester
        );


        TournamentRound round =
                roundRepository
                        .findByTournamentAndRoundNumber(
                                tournament,
                                roundNumber
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Tournament round not found."
                                )
                        );


        return activateRound(
                round
        );
    }

    // =========================================================
    // READ
    // =========================================================

    public List<TournamentParticipant> getParticipants(
            Tournament tournament
    ) {

        return participantRepository
                .findByTournament(
                        tournament
                );
    }

    public List<TournamentParticipant> getStandings(
            Tournament tournament
    ) {

        List<TournamentParticipant> participants =
                participantRepository
                        .findByTournament(
                                tournament
                        );

        List<TournamentMatch> matches =
                matchRepository
                        .findByTournament(
                                tournament
                        );

        return leaderboardService
                .calculateStandings(
                        tournament,
                        participants,
                        matches
                );
    }

    public List<Tournament> getAllTournaments() {

        return tournamentRepository
                .findAll();
    }

    public Tournament getTournament(
            Long tournamentId
    ) {

        return tournamentRepository
                .findById(
                        tournamentId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Tournament not found."
                        )
                );
    }

    public List<TournamentMatch> getRoundMatches(
            Tournament tournament,
            Integer roundNumber
    ) {

        return matchRepository
                .findByTournamentAndRoundNumberOrderByBoardNumber(
                        tournament,
                        roundNumber
                );
    }

    public List<TournamentRound> getRounds(
            Tournament tournament
    ) {

        return roundRepository
                .findByTournamentOrderByRoundNumberAsc(
                        tournament
                );
    }

    // =========================================================
    // RECORD GAME RESULT
    // =========================================================

    @Transactional
    public void recordGameResult(
            Game game
    ) {

        if (game.getTournamentMatch() == null) {

            throw new IllegalArgumentException(
                    "Game does not belong to a tournament match."
            );
        }

        if (game.getStatus()
                != GameStatus.FINISHED) {

            throw new IllegalStateException(
                    "Only a finished game can update a tournament match."
            );
        }

        if (game.getResult() == null) {

            throw new IllegalStateException(
                    "Finished game must have a result."
            );
        }

        TournamentMatch match =
                game.getTournamentMatch();

        if (match.getStatus()
                == TournamentMatchStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Tournament match is already completed."
            );
        }

        Tournament tournament =
                match.getTournament();

        if (tournament.getStatus()
                != TournamentStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Tournament is not in progress."
            );
        }

        if (tournament.getFormat()
                == TournamentFormat.SINGLE_ELIMINATION) {

            recordKnockoutGameResult(
                    match,
                    game
            );

        } else {

            recordStandardGameResult(
                    match,
                    game
            );
        }

        handleRoundCompletion(
                tournament
        );

        tournament.touch();
    }

    // =========================================================
    // SEEDING
    // =========================================================

    private void assignSeeds(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        TimeControlType type =
                tournament
                        .getTimeControl()
                        .getType();

        /*
         * Snapshot rating when the tournament starts.
         */
        for (TournamentParticipant participant
                : participants) {

            int rating =
                    ratingService
                            .getRating(
                                    participant.getUser(),
                                    type
                            );

            participant.setRatingAtStart(
                    rating
            );
        }

        List<TournamentParticipant> sorted =
                new ArrayList<>(
                        participants
                );

        sorted.sort(
                Comparator
                        .comparingInt(
                                (TournamentParticipant participant) ->
                                        participant.getRatingAtStart()
                        )
                        .reversed()
                        .thenComparing(
                                participant ->
                                        participant
                                                .getUser()
                                                .getUsername(),
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(
                                participant ->
                                        participant
                                                .getUser()
                                                .getId()
                        )
        );

        for (int i = 0;
             i < sorted.size();
             i++) {

            sorted.get(i)
                    .setSeed(
                            i + 1
                    );
        }
    }

    // =========================================================
    // PARTICIPANT HELPERS
    // =========================================================

    private boolean isFull(
            Tournament tournament
    ) {

        if (tournament.getMaxPlayers() == null) {
            return false;
        }

        long activeParticipants =
                participantRepository
                        .countByTournamentAndStatus(
                                tournament,
                                TournamentParticipantStatus.ACTIVE
                        );

        return activeParticipants
                >= tournament.getMaxPlayers();
    }

    private List<TournamentParticipant>
    getActiveParticipants(
            Tournament tournament
    ) {

        return participantRepository
                .findByTournament(
                        tournament
                )
                .stream()
                .filter(participant ->
                        participant.getStatus()
                                == TournamentParticipantStatus.ACTIVE
                )
                .toList();
    }

    private TournamentParticipant getParticipant(
            Tournament tournament,
            User user
    ) {

        return participantRepository
                .findByTournamentAndUser(
                        tournament,
                        user
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User is not registered for this tournament."
                        )
                );
    }

    private void validateCreator(
            Tournament tournament,
            User requester
    ) {

        if (!tournament
                .getCreator()
                .getId()
                .equals(
                        requester.getId()
                )) {

            throw new IllegalStateException(
                    "Only the tournament creator can perform this action."
            );
        }
    }

    // =========================================================
    // AUTOMATIC RESULTS
    // =========================================================

    private void resolveAutomaticResults(
            Tournament tournament,
            List<TournamentMatch> matches
    ) {

        for (TournamentMatch match : matches) {

            if (match.getStatus()
                    == TournamentMatchStatus.COMPLETED) {

                continue;
            }

            TournamentParticipant white =
                    match.getWhiteParticipant();

            TournamentParticipant black =
                    match.getBlackParticipant();

            // =================================================
            // TRUE BYE
            // =================================================

            if (black == null) {

                double points =
                        white.getStatus()
                                == TournamentParticipantStatus.ACTIVE
                                ? tournament.getByePoints()
                                : 0.0;

                match.setWhiteScore(
                        points
                );

                match.setBlackScore(
                        null
                );

                match.setTermination(
                        TournamentMatchTermination.BYE
                );

                match.setStatus(
                        TournamentMatchStatus.COMPLETED
                );

                addScore(
                        white,
                        points
                );

                continue;
            }

            boolean whiteForfeited =
                    white.getStatus()
                            == TournamentParticipantStatus.FORFEITED;

            boolean blackForfeited =
                    black.getStatus()
                            == TournamentParticipantStatus.FORFEITED;

            // =================================================
            // DOUBLE FORFEIT
            // =================================================

            if (whiteForfeited
                    && blackForfeited) {

                match.setWhiteScore(
                        0.0
                );

                match.setBlackScore(
                        0.0
                );

                match.setTermination(
                        TournamentMatchTermination.DOUBLE_FORFEIT
                );

                match.setStatus(
                        TournamentMatchStatus.COMPLETED
                );

                continue;
            }

            // =================================================
            // WHITE FORFEIT
            // =================================================

            if (whiteForfeited) {

                match.setWhiteScore(
                        0.0
                );

                match.setBlackScore(
                        1.0
                );

                match.setTermination(
                        TournamentMatchTermination.WHITE_FORFEIT
                );

                match.setStatus(
                        TournamentMatchStatus.COMPLETED
                );

                addScore(
                        black,
                        1.0
                );

                continue;
            }

            // =================================================
            // BLACK FORFEIT
            // =================================================

            if (blackForfeited) {

                match.setWhiteScore(
                        1.0
                );

                match.setBlackScore(
                        0.0
                );

                match.setTermination(
                        TournamentMatchTermination.BLACK_FORFEIT
                );

                match.setStatus(
                        TournamentMatchStatus.COMPLETED
                );

                addScore(
                        white,
                        1.0
                );
            }
        }
    }

    private void addScore(
            TournamentParticipant participant,
            double points
    ) {

        double currentScore =
                participant.getScore() == null
                        ? 0.0
                        : participant.getScore();

        participant.setScore(
                currentScore + points
        );
    }

    // =========================================================
    // ROUND VALIDATION
    // =========================================================

    private void validateCurrentRoundCompleted(
            Tournament tournament
    ) {

        List<TournamentMatch> matches =
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                tournament.getCurrentRound()
                        );

        boolean unfinished =
                matches.stream()
                        .anyMatch(match ->
                                match.getStatus()
                                        != TournamentMatchStatus.COMPLETED
                        );

        if (unfinished) {

            throw new IllegalStateException(
                    "The current round must be completed first."
            );
        }
    }

    private void validateTournamentCanStart(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        if (participants.size() < 2) {

            throw new IllegalStateException(
                    "At least two active participants are required."
            );
        }

        if (tournament.getFormat()
                == TournamentFormat.SWISS) {

            Integer numberOfRounds =
                    tournament.getNumberOfRounds();

            if (numberOfRounds == null
                    || numberOfRounds < 1) {

                throw new IllegalStateException(
                        "Swiss tournament must have a valid number of rounds."
                );
            }

            if (numberOfRounds
                    >= participants.size()) {

                throw new IllegalStateException(
                        "A Swiss tournament must have fewer rounds than active participants."
                );
            }
        }
    }

    // =========================================================
    // STANDARD MATCH RESULT
    // =========================================================

    private void recordStandardGameResult(
            TournamentMatch match,
            Game game
    ) {

        switch (game.getResult()) {

            case WHITE_WIN ->
                    completeTournamentMatch(
                            match,
                            1.0,
                            0.0,
                            TournamentMatchTermination.PLAYED
                    );

            case BLACK_WIN ->
                    completeTournamentMatch(
                            match,
                            0.0,
                            1.0,
                            TournamentMatchTermination.PLAYED
                    );

            case DRAW ->
                    completeTournamentMatch(
                            match,
                            0.5,
                            0.5,
                            TournamentMatchTermination.PLAYED
                    );
        }
    }

    private void completeTournamentMatch(
            TournamentMatch match,
            double whiteScore,
            double blackScore,
            TournamentMatchTermination termination
    ) {

        match.setWhiteScore(
                whiteScore
        );

        match.setBlackScore(
                blackScore
        );

        match.setTermination(
                termination
        );

        match.setStatus(
                TournamentMatchStatus.COMPLETED
        );

        addScore(
                match.getWhiteParticipant(),
                whiteScore
        );

        addScore(
                match.getBlackParticipant(),
                blackScore
        );
    }

    // =========================================================
    // KNOCKOUT RESULT
    // =========================================================

    private void recordKnockoutGameResult(
            TournamentMatch match,
            Game game
    ) {

        switch (game.getResult()) {

            case WHITE_WIN ->
                    completeKnockoutMatch(
                            match,
                            match.getWhiteParticipant(),
                            match.getBlackParticipant(),
                            1.0,
                            0.0
                    );

            case BLACK_WIN ->
                    completeKnockoutMatch(
                            match,
                            match.getBlackParticipant(),
                            match.getWhiteParticipant(),
                            0.0,
                            1.0
                    );

            case DRAW -> {
                /*
                 * The Game is finished, but the
                 * TournamentMatch remains unresolved.
                 *
                 * Another Game may later be created.
                 */
            }
        }
    }

    private void completeKnockoutMatch(
            TournamentMatch match,
            TournamentParticipant winner,
            TournamentParticipant loser,
            double whiteScore,
            double blackScore
    ) {

        match.setWhiteScore(
                whiteScore
        );

        match.setBlackScore(
                blackScore
        );

        match.setTermination(
                TournamentMatchTermination.PLAYED
        );

        match.setStatus(
                TournamentMatchStatus.COMPLETED
        );

        loser.setStatus(
                TournamentParticipantStatus.ELIMINATED
        );
    }

    // =========================================================
    // ROUND COMPLETION
    // =========================================================

    private void handleRoundCompletion(
            Tournament tournament
    ) {

        if (tournament.getStatus()
                != TournamentStatus.IN_PROGRESS) {

            return;
        }

        Integer currentRound =
                tournament.getCurrentRound();

        if (currentRound == null) {
            return;
        }

        List<TournamentMatch> currentRoundMatches =
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                currentRound
                        );

        if (currentRoundMatches.isEmpty()) {
            return;
        }

        boolean roundCompleted =
                currentRoundMatches.stream()
                        .allMatch(match ->
                                match.getStatus()
                                        == TournamentMatchStatus.COMPLETED
                        );

        if (!roundCompleted) {
            return;
        }

        completeRound(
                tournament,
                currentRound
        );

        switch (tournament.getFormat()) {

            case SINGLE_ELIMINATION ->
                    handleSingleEliminationRoundCompletion(
                            tournament
                    );

            case ROUND_ROBIN ->
                    handleRoundRobinRoundCompletion(
                            tournament
                    );

            case SWISS ->
                    handleSwissRoundCompletion(
                            tournament
                    );
        }
    }

    private void handleSingleEliminationRoundCompletion(
            Tournament tournament
    ) {

        List<TournamentParticipant> activeParticipants =
                getActiveParticipants(
                        tournament
                );

        /*
         * Exactly one ACTIVE participant remains
         * only after the final round has been resolved.
         */
        if (activeParticipants.size() == 1) {

            finishTournament(
                    tournament
            );

            return;
        }

        if (activeParticipants.isEmpty()) {

            throw new IllegalStateException(
                    "Single-elimination tournament has no remaining active participant."
            );
        }

        scheduleNextRound(
                tournament
        );
    }

    private void handleSwissRoundCompletion(
            Tournament tournament
    ) {

        if (tournament.getCurrentRound()
                >= tournament.getNumberOfRounds()) {

            finishTournament(
                    tournament
            );

            return;
        }

        scheduleNextRound(
                tournament
        );
    }

    private void handleRoundRobinRoundCompletion(
            Tournament tournament
    ) {

        int nextRound =
                tournament.getCurrentRound() + 1;

        List<TournamentMatch> nextRoundMatches =
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                nextRound
                        );

        if (nextRoundMatches.isEmpty()) {

            finishTournament(
                    tournament
            );

            return;
        }

        scheduleNextRound(
                tournament
        );
    }

    // =========================================================
    // THIRD PLACE
    // =========================================================

    private boolean isCurrentThirdPlaceParticipant(
            TournamentParticipant participant,
            List<TournamentMatch> matches
    ) {

        return matches.stream()
                .filter(match ->
                        match.getType()
                                == TournamentMatchType.THIRD_PLACE
                )
                .filter(match ->
                        match.getStatus()
                                != TournamentMatchStatus.COMPLETED
                )
                .anyMatch(match ->
                        sameParticipant(
                                match.getWhiteParticipant(),
                                participant
                        )
                                ||
                                sameParticipant(
                                        match.getBlackParticipant(),
                                        participant
                                )
                );
    }

    private boolean sameParticipant(
            TournamentParticipant first,
            TournamentParticipant second
    ) {

        if (first == second) {
            return true;
        }

        if (first == null
                || second == null) {

            return false;
        }

        if (first.getId() != null
                && second.getId() != null) {

            return first.getId()
                    .equals(
                            second.getId()
                    );
        }

        return false;
    }

    // =========================================================
    // FINISH TOURNAMENT
    // =========================================================

    private void finishTournament(
            Tournament tournament
    ) {

        assignFinalRanks(
                tournament
        );

        tournament.setStatus(
                TournamentStatus.FINISHED
        );

        tournament.setFinishedAt(
                LocalDateTime.now()
        );
    }

    // =========================================================
    // FINAL RANKING
    // =========================================================

    private void assignFinalRanks(
            Tournament tournament
    ) {

        List<TournamentParticipant> allParticipants =
                participantRepository
                        .findByTournament(
                                tournament
                        );

        /*
         * Reset first.
         *
         * WITHDRAWN participants and participants
         * eliminated outside the Top 4 in knockout
         * therefore remain with null finalRank.
         */
        allParticipants.forEach(participant ->
                participant.setFinalRank(
                        null
                )
        );

        List<TournamentParticipant> participants =
                allParticipants.stream()
                        .filter(participant ->
                                participant.getStatus()
                                        != TournamentParticipantStatus.WITHDRAWN
                        )
                        .toList();

        if (tournament.getFormat()
                == TournamentFormat.SINGLE_ELIMINATION) {

            assignSingleEliminationFinalRanks(
                    tournament,
                    participants
            );

            return;
        }

        assignLeaderboardFinalRanks(
                tournament,
                participants
        );
    }

    // =========================================================
    // SWISS / ROUND ROBIN FINAL RANKING
    // =========================================================

    private void assignLeaderboardFinalRanks(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        List<TournamentMatch> matches =
                matchRepository
                        .findByTournament(
                                tournament
                        );

        List<TournamentParticipant> standings =
                leaderboardService
                        .calculateStandings(
                                tournament,
                                participants,
                                matches
                        );

        for (int i = 0;
             i < standings.size();
             i++) {

            standings.get(i)
                    .setFinalRank(
                            i + 1
                    );
        }
    }

    // =========================================================
    // SINGLE ELIMINATION FINAL RANKING
    // =========================================================

    private void assignSingleEliminationFinalRanks(
            Tournament tournament,
            List<TournamentParticipant> participants
    ) {

        List<TournamentMatch> finalRoundMatches =
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                tournament.getCurrentRound()
                        );

        TournamentMatch finalMatch =
                finalRoundMatches.stream()
                        .filter(
                                this::isMainMatch
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Single-elimination tournament has no final match."
                                )
                        );

        assignRanksFromMatch(
                finalMatch,
                1,
                2
        );

        TournamentMatch thirdPlaceMatch =
                finalRoundMatches.stream()
                        .filter(match ->
                                match.getType()
                                        == TournamentMatchType.THIRD_PLACE
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        if (thirdPlaceMatch != null) {

            assignRanksFromMatch(
                    thirdPlaceMatch,
                    3,
                    4
            );

            return;
        }

        /*
         * Special case:
         *
         * With three players one semifinal is a BYE.
         * Therefore there is only one semifinal loser
         * and no third-place match.
         */
        List<TournamentParticipant> unrankedParticipants =
                participants.stream()
                        .filter(participant ->
                                participant.getFinalRank()
                                        == null
                        )
                        .toList();

        if (unrankedParticipants.size() == 1) {

            unrankedParticipants
                    .get(0)
                    .setFinalRank(
                            3
                    );
        }
    }

    private boolean isMainMatch(
            TournamentMatch match
    ) {

        /*
         * null also counts as MAIN for compatibility
         * with older tests/entities created before
         * TournamentMatchType was introduced.
         */
        return match.getType() == null
                ||
                match.getType()
                        == TournamentMatchType.MAIN;
    }

    private void assignRanksFromMatch(
            TournamentMatch match,
            int winnerRank,
            int loserRank
    ) {

        if (match.getStatus()
                != TournamentMatchStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Final ranking requires a completed match."
            );
        }

        TournamentParticipant white =
                match.getWhiteParticipant();

        TournamentParticipant black =
                match.getBlackParticipant();

        if (white == null
                || black == null) {

            throw new IllegalStateException(
                    "A placement match must have two participants."
            );
        }

        Double whiteScore =
                match.getWhiteScore();

        Double blackScore =
                match.getBlackScore();

        if (whiteScore == null
                || blackScore == null) {

            throw new IllegalStateException(
                    "A completed placement match must have scores."
            );
        }

        if (whiteScore > blackScore) {

            white.setFinalRank(
                    winnerRank
            );

            black.setFinalRank(
                    loserRank
            );

            return;
        }

        if (blackScore > whiteScore) {

            black.setFinalRank(
                    winnerRank
            );

            white.setFinalRank(
                    loserRank
            );

            return;
        }

        throw new IllegalStateException(
                "A knockout placement match cannot end in a draw."
        );
    }

    private void startRound(
            Tournament tournament,
            Integer roundNumber,
            LocalDateTime scheduledAt
    ) {

        LocalDateTime now =
                LocalDateTime.now();


        TournamentRound round =
                roundRepository
                        .findByTournamentAndRoundNumber(
                                tournament,
                                roundNumber
                        )
                        .orElseGet(() ->
                                TournamentRound.builder()
                                        .tournament(
                                                tournament
                                        )
                                        .roundNumber(
                                                roundNumber
                                        )
                                        .build()
                        );


        round.setStatus(
                TournamentRoundStatus.IN_PROGRESS
        );


        if (round.getScheduledAt() == null) {

            round.setScheduledAt(
                    scheduledAt
            );
        }


        if (round.getStartedAt() == null) {

            round.setStartedAt(
                    now
            );
        }


        roundRepository.save(
                round
        );
    }

    private void completeRound(
            Tournament tournament,
            Integer roundNumber
    ) {

        TournamentRound round =
                roundRepository
                        .findByTournamentAndRoundNumber(
                                tournament,
                                roundNumber
                        )
                        .orElseGet(() ->
                                TournamentRound.builder()
                                        .tournament(
                                                tournament
                                        )
                                        .roundNumber(
                                                roundNumber
                                        )
                                        .startedAt(
                                                LocalDateTime.now()
                                        )
                                        .build()
                        );


        round.setStatus(
                TournamentRoundStatus.COMPLETED
        );

        round.setCompletedAt(
                LocalDateTime.now()
        );


        roundRepository.save(
                round
        );
    }

    private void scheduleRound(
            Tournament tournament,
            Integer roundNumber,
            LocalDateTime scheduledAt
    ) {

        TournamentRound round =
                roundRepository
                        .findByTournamentAndRoundNumber(
                                tournament,
                                roundNumber
                        )
                        .orElseGet(() ->
                                TournamentRound.builder()
                                        .tournament(
                                                tournament
                                        )
                                        .roundNumber(
                                                roundNumber
                                        )
                                        .build()
                        );

        round.setStatus(
                TournamentRoundStatus.SCHEDULED
        );

        round.setScheduledAt(
                scheduledAt
        );

        round.setStartedAt(
                null
        );

        round.setCompletedAt(
                null
        );

        roundRepository.save(
                round
        );
    }

    private Tournament activateRound(
            TournamentRound round
    ) {

        if (round.getStatus()
                != TournamentRoundStatus.SCHEDULED) {

            throw new IllegalStateException(
                    "Only a scheduled round can be activated."
            );
        }


        Tournament tournament =
                round.getTournament();


        if (tournament.getStatus()
                != TournamentStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Tournament is not in progress."
            );
        }


        int expectedRound =
                tournament.getCurrentRound() + 1;


        if (!round.getRoundNumber()
                .equals(
                        expectedRound
                )) {

            throw new IllegalStateException(
                    "Tournament round is not the next round."
            );
        }


        List<TournamentMatch> matches =
                matchRepository
                        .findByTournamentAndRoundNumberOrderByBoardNumber(
                                tournament,
                                round.getRoundNumber()
                        );


        if (matches.isEmpty()) {

            throw new IllegalStateException(
                    "Scheduled round has no matches."
            );
        }


        tournament.setCurrentRound(
                round.getRoundNumber()
        );


        round.setStatus(
                TournamentRoundStatus.IN_PROGRESS
        );

        round.setStartedAt(
                LocalDateTime.now()
        );


        roundRepository.save(
                round
        );


        resolveAutomaticResults(
                tournament,
                matches
        );


        handleRoundCompletion(
                tournament
        );


        tournament.touch();

        return tournament;
    }
}