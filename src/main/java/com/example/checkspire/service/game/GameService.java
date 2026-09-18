package com.example.checkspire.service.game;

import com.example.checkspire.model.entity.challenge.Challenge;
import com.example.checkspire.model.entity.game.Game;
import com.example.checkspire.model.entity.game.GameMove;
import com.example.checkspire.model.entity.tournament.Tournament;
import com.example.checkspire.model.entity.tournament.TournamentMatch;
import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.model.enums.challenge.ChallengeStatus;
import com.example.checkspire.model.enums.game.GameResult;
import com.example.checkspire.model.enums.game.GameStatus;
import com.example.checkspire.model.enums.game.GameTermination;
import com.example.checkspire.model.enums.timeControl.TimeControl;
import com.example.checkspire.model.enums.timeControl.TimeControlType;
import com.example.checkspire.model.enums.tournament.TournamentMatchStatus;
import com.example.checkspire.model.enums.tournament.TournamentStatus;
import com.example.checkspire.repository.game.GameMoveRepository;
import com.example.checkspire.repository.game.GameRepository;
import com.example.checkspire.service.game.dto.GameMoveResponse;
import com.example.checkspire.service.game.dto.GamePageResponse;
import com.example.checkspire.service.game.dto.GameStateResponse;
import com.example.checkspire.service.game.dto.GameSummaryResponse;
import com.example.checkspire.service.game.engine.GameEngine;
import com.example.checkspire.service.game.engine.history.RepetitionTracker;
import com.example.checkspire.service.game.engine.model.*;
import com.example.checkspire.service.game.notation.PgnGenerator;
import com.example.checkspire.service.tournament.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;
    private final RatingService ratingService;
    private final TournamentService tournamentService;
    private final GameEngine gameEngine;
    private final GameMoveRepository gameMoveRepository;
    private final Clock clock;
    private final PgnGenerator pgnGenerator;
    private final GameStateMapper gameStateMapper;
    private final GameSummaryMapper gameSummaryMapper;

    @Transactional
    public Game createGameFromChallenge(Challenge challenge) {

        if (challenge.getStatus() != ChallengeStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "A game can only be created from an accepted challenge."
            );
        }

        boolean challengerIsWhite = switch (challenge.getColorPreference()) {
            case WHITE -> true;
            case BLACK -> false;
            case RANDOM -> ThreadLocalRandom.current().nextBoolean();
        };

        User whitePlayer = challengerIsWhite
                ? challenge.getChallenger()
                : challenge.getOpponent();

        User blackPlayer = challengerIsWhite
                ? challenge.getOpponent()
                : challenge.getChallenger();

        return createGame(
                whitePlayer,
                blackPlayer,
                challenge.getTimeControl(),
                challenge.isRated(),
                null
        );
    }

    @Transactional
    public Game createGameFromTournament(TournamentMatch match) {

        if (match.getBlackParticipant() == null) {
            throw new IllegalStateException(
                    "A bye match cannot create a game."
            );
        }

        Tournament tournament = match.getTournament();

        return createGame(
                match.getWhiteParticipant().getUser(),
                match.getBlackParticipant().getUser(),
                tournament.getTimeControl(),
                tournament.isRated(),
                match
        );
    }

    @Transactional
    public void startGame(Game game) {

        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException(
                    "Only a waiting game can be started."
            );
        }

        if (hasActiveGame(game.getWhitePlayer())) {
            throw new IllegalStateException(
                    "White player already has an active game."
            );
        }

        if (hasActiveGame(game.getBlackPlayer())) {
            throw new IllegalStateException(
                    "Black player already has an active game."
            );
        }

        TournamentMatch tournamentMatch =
                game.getTournamentMatch();

        if (tournamentMatch != null) {

            if (tournamentMatch.getStatus()
                    == TournamentMatchStatus.COMPLETED) {

                throw new IllegalStateException(
                        "A game cannot be started for a completed tournament match."
                );
            }

            if (tournamentMatch.getStatus()
                    == TournamentMatchStatus.PENDING) {

                tournamentMatch.setStatus(
                        TournamentMatchStatus.IN_PROGRESS
                );
            }
        }

        LocalDateTime now =
                LocalDateTime.now(clock);

        long initialTimeMillis =
                game.getTimeControl()
                        .getInitialTimeSeconds()
                        * 1000L;

        game.setStatus(
                GameStatus.IN_PROGRESS
        );

        game.setStartedAt(
                now
        );

        game.setTurnStartedAt(
                now
        );

        game.setWhiteTimeRemainingMillis(
                initialTimeMillis
        );

        game.setBlackTimeRemainingMillis(
                initialTimeMillis
        );

        game.setTurnExpiresAt(
                now.plus(
                        Duration.ofMillis(
                                initialTimeMillis
                        )
                )
        );

        setRatingSnapshots(
                game
        );
    }

    @Transactional
    public void startGamesForCurrentRound(
            Tournament tournament
    ) {

        if (tournament.getStatus()
                != TournamentStatus.IN_PROGRESS) {

            return;
        }

        if (tournament.getCurrentRound() == null) {
            return;
        }


        List<TournamentMatch> matches =
                tournamentService
                        .getRoundMatches(
                                tournament,
                                tournament.getCurrentRound()
                        );


        for (TournamentMatch match : matches) {

            if (match.getStatus()
                    == TournamentMatchStatus.COMPLETED) {

                continue;
            }


            if (match.getBlackParticipant() == null) {

                continue;
            }


            boolean hasActiveGame =
                    gameRepository
                            .findByTournamentMatchOrderById(
                                    match
                            )
                            .stream()
                            .anyMatch(game ->
                                    game.getStatus()
                                            == GameStatus.WAITING
                                            ||
                                            game.getStatus()
                                                    == GameStatus.IN_PROGRESS
                            );


            if (hasActiveGame) {
                continue;
            }


            Game game =
                    createGameFromTournament(
                            match
                    );

            startGame(
                    game
            );
        }
    }

    @Transactional
    public void finishGame(
            Game game,
            GameResult result,
            GameTermination termination
    ) {

        finishGameInternal(
                game,
                result,
                termination,
                true
        );
    }

    @Transactional
    public Game abortGame(
            Long gameId,
            User player
    ) {

        Game game =
                gameRepository
                        .findByIdForUpdate(
                                gameId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Game not found."
                                )
                        );


        if (
                game.getStatus()
                        != GameStatus.IN_PROGRESS
        ) {

            throw new IllegalStateException(
                    "Only an active game can be aborted."
            );
        }


        validateParticipant(
                game,
                player
        );


        if (
                game.getTournamentMatch()
                        != null
        ) {

            throw new IllegalStateException(
                    "Tournament games cannot be aborted."
            );
        }


        List<GameMove> moves =
                gameMoveRepository
                        .findByGameIdOrderByPlyNumberAsc(
                                gameId
                        );


        if (!moves.isEmpty()) {

            throw new IllegalStateException(
                    "A game can only be aborted before the first move."
            );
        }


        game.setStatus(
                GameStatus.ABORTED
        );

        game.setTermination(
                GameTermination.ABORTED
        );

        game.setFinishedAt(
                LocalDateTime.now(
                        clock
                )
        );

        game.setTurnStartedAt(
                null
        );

        game.setTurnExpiresAt(
                null
        );

        game.setDrawOfferBy(
                null
        );


        game.setWhiteRatingAfter(
                game.getWhiteRatingBefore()
        );

        game.setBlackRatingAfter(
                game.getBlackRatingBefore()
        );


        return gameRepository.save(
                game
        );
    }

    public List<GameSummaryResponse> getGameSummariesForUser(
            User user
    ) {

        return gameRepository
                .findByWhitePlayerOrBlackPlayerOrderByStartedAtDesc(
                        user,
                        user
                )
                .stream()
                .map(game ->
                        gameSummaryMapper.toResponse(
                                game,
                                user
                        )
                )
                .toList();
    }

    public List<Game> getGamesForUser(User user) {
        return gameRepository
                .findByWhitePlayerOrBlackPlayerOrderByStartedAtDesc(
                        user,
                        user
                );
    }

    @Transactional
    public Game makeMove(
            Long gameId,
            User player,
            Move move
    ) {

        Game game =
                gameRepository.findByIdForUpdate(
                        gameId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Game not found."
                        )
                );

        if (game.getStatus()
                != GameStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Game is not in progress."
            );
        }

        validateParticipant(
                game,
                player
        );

        GameState state =
                loadGameState(
                        game
                );

        validatePlayerTurn(
                game,
                player,
                state
        );

        PieceColor movingColor =
                state.getSideToMove();

        LocalDateTime now =
                LocalDateTime.now(
                        clock
                );

        if (hasTimedOut(
                game,
                state,
                movingColor,
                now
        )) {

            return gameRepository.save(
                    game
            );
        }

        return executeMove(
                game,
                player,
                state,
                move,
                movingColor,
                now
        );
    }

    @Transactional
    public Game makeMove(
            Long gameId,
            User player,
            String from,
            String to,
            PieceType promotion
    ) {

        Game game =
                gameRepository.findByIdForUpdate(
                        gameId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Game not found."
                        )
                );

        if (game.getStatus()
                != GameStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Game is not in progress."
            );
        }

        validateParticipant(
                game,
                player
        );

        GameState state =
                loadGameState(
                        game
                );

        validatePlayerTurn(
                game,
                player,
                state
        );

        PieceColor movingColor =
                state.getSideToMove();

        LocalDateTime now =
                LocalDateTime.now(
                        clock
                );

        if (hasTimedOut(
                game,
                state,
                movingColor,
                now
        )) {

            return gameRepository.save(
                    game
            );
        }
        Result result = new Result(game, state, movingColor, now);

        Square fromSquare =
                Square.fromAlgebraic(
                        from
                );

        Square toSquare =
                Square.fromAlgebraic(
                        to
                );

        Move move =
                gameEngine.resolveMove(
                        result.state(),
                        fromSquare,
                        toSquare,
                        promotion
                );

        return executeMove(
                result.game(),
                player,
                result.state(),
                move,
                result.movingColor(),
                result.now()
        );
    }

    private record Result(Game game, GameState state, PieceColor movingColor, LocalDateTime now) {
    }

    @Transactional
    public Game resignGame(
            Long gameId,
            User player
    ) {

        Game game =
                getActiveGame(
                        gameId
                );

        validateParticipant(
                game,
                player
        );

        GameResult result;

        if (game.getWhitePlayer()
                .getId()
                .equals(player.getId())) {

            result =
                    GameResult.BLACK_WIN;

        } else {

            result =
                    GameResult.WHITE_WIN;
        }

        finishGame(
                game,
                result,
                GameTermination.RESIGNATION
        );

        return gameRepository.save(
                game
        );
    }

    @Transactional
    public Game forfeitTournamentGame(
            Long gameId,
            User player
    ) {

        Game game =
                getActiveGame(
                        gameId
                );


        validateParticipant(
                game,
                player
        );


        if (game.getTournamentMatch() == null) {

            throw new IllegalStateException(
                    "Game does not belong to a tournament."
            );
        }


        GameResult result;

        if (game.getWhitePlayer()
                .getId()
                .equals(
                        player.getId()
                )) {

            result =
                    GameResult.BLACK_WIN;

        } else {

            result =
                    GameResult.WHITE_WIN;
        }


        finishGameInternal(
                game,
                result,
                GameTermination.RESIGNATION,
                false
        );


        return gameRepository.save(
                game
        );
    }

    @Transactional
    public Game offerDraw(
            Long gameId,
            User player
    ) {

        Game game =
                getActiveGame(
                        gameId
                );

        validateParticipant(
                game,
                player
        );

        if (game.getDrawOfferBy() != null) {

            throw new IllegalStateException(
                    "There is already an active draw offer."
            );
        }

        game.setDrawOfferBy(
                player
        );

        return gameRepository.save(
                game
        );
    }

    @Transactional
    public Game acceptDraw(
            Long gameId,
            User player
    ) {

        Game game =
                getActiveGame(
                        gameId
                );

        validateParticipant(
                game,
                player
        );

        User offerBy =
                game.getDrawOfferBy();

        if (offerBy == null) {

            throw new IllegalStateException(
                    "There is no active draw offer."
            );
        }

        if (offerBy.getId()
                .equals(player.getId())) {

            throw new IllegalStateException(
                    "A player cannot accept their own draw offer."
            );
        }

        game.setDrawOfferBy(
                null
        );

        finishGame(
                game,
                GameResult.DRAW,
                GameTermination.AGREEMENT
        );

        return gameRepository.save(
                game
        );
    }

    @Transactional
    public Game rejectDraw(
            Long gameId,
            User player
    ) {

        Game game =
                getActiveGame(
                        gameId
                );

        validateParticipant(
                game,
                player
        );

        User offerBy =
                game.getDrawOfferBy();

        if (offerBy == null) {

            throw new IllegalStateException(
                    "There is no active draw offer."
            );
        }

        if (offerBy.getId()
                .equals(player.getId())) {

            throw new IllegalStateException(
                    "A player cannot reject their own draw offer."
            );
        }

        game.setDrawOfferBy(
                null
        );

        return gameRepository.save(
                game
        );
    }

    @Transactional
    public Game timeoutGame(
            Long gameId,
            User timedOutPlayer
    ) {

        Game game =
                getActiveGame(
                        gameId
                );

        validateParticipant(
                game,
                timedOutPlayer
        );

        GameState state =
                loadGameState(
                        game
                );

        PieceColor timedOutColor;

        if (game.getWhitePlayer()
                .getId()
                .equals(timedOutPlayer.getId())) {

            timedOutColor =
                    PieceColor.WHITE;

            game.setWhiteTimeRemainingMillis(
                    0L
            );

        } else {

            timedOutColor =
                    PieceColor.BLACK;

            game.setBlackTimeRemainingMillis(
                    0L
            );
        }

        finishOnTimeout(
                game,
                state,
                timedOutColor
        );

        return gameRepository.save(
                game
        );
    }

    @Transactional
    public boolean finalizeTimeoutIfExpired(
            Long gameId
    ) {

        Game game =
                gameRepository.findByIdForUpdate(
                        gameId
                ).orElse(
                        null
                );

        if (game == null) {
            return false;
        }

        if (game.getStatus()
                != GameStatus.IN_PROGRESS) {

            return false;
        }

        LocalDateTime turnExpiresAt =
                game.getTurnExpiresAt();

        if (turnExpiresAt == null) {
            return false;
        }

        LocalDateTime now =
                LocalDateTime.now(
                        clock
                );

        if (turnExpiresAt.isAfter(
                now
        )) {
            return false;
        }

        GameState state =
                loadGameState(
                        game
                );

        PieceColor timedOutColor =
                state.getSideToMove();

        if (timedOutColor
                == PieceColor.WHITE) {

            game.setWhiteTimeRemainingMillis(
                    0L
            );

        } else {

            game.setBlackTimeRemainingMillis(
                    0L
            );
        }

        finishOnTimeout(
                game,
                state,
                timedOutColor
        );

        gameRepository.save(
                game
        );

        return true;
    }

    @Transactional
    public GameStateResponse makeMoveAndGetState(
            Long gameId,
            User player,
            String from,
            String to,
            PieceType promotion
    ) {

        Game game =
                makeMove(
                        gameId,
                        player,
                        from,
                        to,
                        promotion
                );

        return gameStateMapper.toResponse(
                game
        );
    }

    @Transactional
    public Optional<GameStateResponse>
    finalizeTimeoutIfExpiredAndGetState(
            Long gameId
    ) {

        boolean finalized =
                finalizeTimeoutIfExpired(
                        gameId
                );

        if (!finalized) {
            return Optional.empty();
        }

        Game game =
                gameRepository.findById(
                        gameId
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "Finalized game was not found."
                        )
                );

        return Optional.of(
                gameStateMapper.toResponse(
                        game
                )
        );
    }

    // =========================================================
    // READ
    // =========================================================

    public Optional<GamePageResponse> getGamePage(
            Long gameId,
            User viewer
    ) {

        Optional<Game> gameOptional =
                gameRepository.findById(
                        gameId
                );

        if (gameOptional.isEmpty()) {
            return Optional.empty();
        }

        Game game =
                gameOptional.get();


        PieceColor playerColor =
                resolvePlayerColor(
                        game,
                        viewer
                );


        boolean viewerParticipant =
                playerColor != null;


        PieceColor viewerColor =
                viewerParticipant
                        ? playerColor
                        : PieceColor.WHITE;


        ClockSnapshot clockSnapshot =
                calculateClockSnapshot(
                        game
                );


        Long tournamentId =
                game.getTournamentMatch() != null
                        ? game.getTournamentMatch()
                        .getTournament()
                        .getId()
                        : null;


        return Optional.of(
                new GamePageResponse(
                        gameStateMapper.toResponse(
                                game
                        ),
                        viewerColor,
                        clockSnapshot.whiteMillis(),
                        clockSnapshot.blackMillis(),
                        tournamentId,
                        viewerParticipant
                )
        );
    }

    public List<GameMoveResponse> getMoveHistory(
            Long gameId
    ) {

        gameRepository
                .findById(
                        gameId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Game not found."
                        )
                );


        return gameMoveRepository
                .findByGameIdOrderByPlyNumberAsc(
                        gameId
                )
                .stream()
                .map(move ->
                        new GameMoveResponse(
                                move.getPlyNumber(),
                                move.getSan(),
                                move.getFromSquare(),
                                move.getToSquare(),
                                move.getFenAfter()
                        )
                )
                .toList();
    }

    public Optional<Game> getLatestGameForTournamentMatch(
            TournamentMatch match
    ) {

        List<Game> games =
                gameRepository
                        .findByTournamentMatchOrderById(
                                match
                        );

        if (games.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                games.get(
                        games.size() - 1
                )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private record ClockSnapshot(

            long whiteMillis,

            long blackMillis

    ) {
    }

    private void setRatingSnapshots(Game game) {

        TimeControlType type = game.getTimeControl().getType();

        int whiteRating = ratingService.getRating(
                game.getWhitePlayer(),
                type
        );

        int blackRating = ratingService.getRating(
                game.getBlackPlayer(),
                type
        );

        game.setWhiteRatingBefore(whiteRating);
        game.setBlackRatingBefore(blackRating);
    }

    private boolean hasActiveGame(User player) {
        return gameRepository.existsByPlayerAndStatus(
                player,
                GameStatus.IN_PROGRESS
        );
    }

    private Game createGame(
            User whitePlayer,
            User blackPlayer,
            TimeControl timeControl,
            boolean rated,
            TournamentMatch tournamentMatch
    ) {

        Game game =
                Game.builder()
                        .whitePlayer(whitePlayer)
                        .blackPlayer(blackPlayer)
                        .timeControl(timeControl)
                        .rated(rated)
                        .status(GameStatus.WAITING)
                        .tournamentMatch(tournamentMatch)
                        .build();

        initializeGameState(game);

        return gameRepository.save(game);
    }

    private GameState loadGameState(
            Game game
    ) {

        String currentFen =
                game.getCurrentFen();

        if (currentFen == null
                || currentFen.isBlank()) {

            throw new IllegalStateException(
                    "Game does not have a current FEN."
            );
        }

        return gameEngine.fromFen(
                currentFen
        );
    }

    private void validatePlayerTurn(
            Game game,
            User player,
            GameState state
    ) {

        User expectedPlayer =
                state.getSideToMove()
                        == PieceColor.WHITE
                        ? game.getWhitePlayer()
                        : game.getBlackPlayer();

        if (!expectedPlayer.getId()
                .equals(player.getId())) {

            throw new IllegalStateException(
                    "It is not this player's turn."
            );
        }
    }

    private void initializeGameState(
            Game game
    ) {

        game.setCurrentFen(
                gameEngine.toFen(
                        GameState.initial()
                )
        );


        long initialTimeMillis =
                game.getTimeControl()
                        .getInitialTimeSeconds()
                        * 1000L;


        game.setWhiteTimeRemainingMillis(
                initialTimeMillis
        );

        game.setBlackTimeRemainingMillis(
                initialTimeMillis
        );


        game.setWhiteTimeRemainingMillis(
                (long)game.getTimeControl()
                        .getInitialTimeSeconds()
        );

        game.setBlackTimeRemainingMillis(
                (long)game.getTimeControl()
                        .getInitialTimeSeconds()
        );
    }

    private void finishIfGameEnded(
            Game game,
            GameState state,
            RepetitionTracker repetitionTracker
    ) {

        if (gameEngine.isCheckmate(state)) {

            GameResult result =
                    state.getSideToMove()
                            == PieceColor.WHITE
                            ? GameResult.BLACK_WIN
                            : GameResult.WHITE_WIN;

            finishGame(
                    game,
                    result,
                    GameTermination.CHECKMATE
            );

            return;
        }


        if (gameEngine.isStalemate(state)) {

            finishGame(
                    game,
                    GameResult.DRAW,
                    GameTermination.STALEMATE
            );

            return;
        }


        if (
                gameEngine.isInsufficientMaterial(
                        state
                )
        ) {

            finishGame(
                    game,
                    GameResult.DRAW,
                    GameTermination.INSUFFICIENT_MATERIAL
            );

            return;
        }


        Set<DrawReason> automaticReasons =
                gameEngine
                        .getAutomaticDrawReasons(
                                state,
                                repetitionTracker
                        );


        if (
                automaticReasons.contains(
                        DrawReason.FIVEFOLD_REPETITION
                )
        ) {

            finishGame(
                    game,
                    GameResult.DRAW,
                    GameTermination.FIVEFOLD_REPETITION
            );

            return;
        }


        if (
                automaticReasons.contains(
                        DrawReason.SEVENTY_FIVE_MOVE_RULE
                )
        ) {

            finishGame(
                    game,
                    GameResult.DRAW,
                    GameTermination.SEVENTY_FIVE_MOVE_RULE
            );

            return;
        }


        Set<DrawReason> claimableReasons =
                gameEngine
                        .getClaimableDrawReasons(
                                state,
                                repetitionTracker
                        );


        if (
                claimableReasons.contains(
                        DrawReason.THREEFOLD_REPETITION
                )
        ) {

            finishGame(
                    game,
                    GameResult.DRAW,
                    GameTermination.THREEFOLD_REPETITION
            );

            return;
        }


        if (
                claimableReasons.contains(
                        DrawReason.FIFTY_MOVE_RULE
                )
        ) {

            finishGame(
                    game,
                    GameResult.DRAW,
                    GameTermination.FIFTY_MOVE_RULE
            );
        }
    }

    private int calculatePlyNumber(
            GameState state
    ) {

        int fullMoveNumber =
                state.getFullMoveNumber();

        return state.getSideToMove()
                == PieceColor.WHITE
                ? (fullMoveNumber - 1) * 2 + 1
                : (fullMoveNumber - 1) * 2 + 2;
    }

    private void saveGameMove(
            Game game,
            Move move,
            int plyNumber,
            String san,
            String fenAfter
    ) {

        GameMove gameMove =
                GameMove.builder()
                        .game(game)
                        .plyNumber(plyNumber)
                        .fromSquare(
                                toAlgebraic(
                                        move.from()
                                )
                        )
                        .toSquare(
                                toAlgebraic(
                                        move.to()
                                )
                        )
                        .moveType(
                                move.type().name()
                        )
                        .promotionPiece(
                                move.promotion() == null
                                        ? null
                                        : move.promotion().name()
                        )
                        .fenAfter(fenAfter)
                        .san(san)
                        .build();

        gameMoveRepository.save(
                gameMove
        );
    }

    private String toAlgebraic(
            Square square
    ) {

        char file =
                (char) ('a' + square.file());

        int rank =
                square.rank() + 1;

        return String.valueOf(file)
                + rank;
    }

    private RepetitionTracker restoreRepetitionTracker(
            Game game
    ) {

        RepetitionTracker tracker =
                gameEngine.createRepetitionTracker(
                        GameState.initial()
                );

        List<GameMove> moves =
                gameMoveRepository
                        .findByGameIdOrderByPlyNumberAsc(
                                game.getId()
                        );

        for (GameMove gameMove : moves) {

            GameState historicalState =
                    gameEngine.fromFen(
                            gameMove.getFenAfter()
                    );

            tracker.recordPosition(
                    historicalState
            );
        }

        return tracker;
    }

    private Game getActiveGame(
            Long gameId
    ) {

        Game game =
                gameRepository.findByIdForUpdate(gameId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Game not found."
                                )
                        );

        if (game.getStatus()
                != GameStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Game is not in progress."
            );
        }

        return game;
    }

    private void validateParticipant(
            Game game,
            User player
    ) {

        boolean isWhite =
                game.getWhitePlayer()
                        .getId()
                        .equals(player.getId());

        boolean isBlack =
                game.getBlackPlayer()
                        .getId()
                        .equals(player.getId());

        if (!isWhite && !isBlack) {

            throw new IllegalArgumentException(
                    "User is not a participant in this game."
            );
        }
    }

    private void clearOpponentDrawOffer(
            Game game,
            User player
    ) {

        User offerBy =
                game.getDrawOfferBy();

        if (offerBy == null) {
            return;
        }

        if (!offerBy.getId()
                .equals(player.getId())) {

            game.setDrawOfferBy(
                    null
            );
        }
    }

    private void updateClockAfterMove(
            Game game,
            PieceColor movingColor,
            LocalDateTime now
    ) {

        long elapsedMillis =
                Duration.between(
                        game.getTurnStartedAt(),
                        now
                ).toMillis();

        long incrementMillis =
                game.getTimeControl()
                        .getIncrementSeconds()
                        * 1000L;

        if (movingColor == PieceColor.WHITE) {

            long remainingMillis =
                    game.getWhiteTimeRemainingMillis()
                            - elapsedMillis
                            + incrementMillis;

            game.setWhiteTimeRemainingMillis(
                    remainingMillis
            );

        } else {

            long remainingMillis =
                    game.getBlackTimeRemainingMillis()
                            - elapsedMillis
                            + incrementMillis;

            game.setBlackTimeRemainingMillis(
                    remainingMillis
            );
        }

        game.setTurnStartedAt(
                now
        );

        PieceColor nextColor =
                movingColor.opposite();

        long nextPlayerRemainingMillis =
                nextColor == PieceColor.WHITE
                        ? game.getWhiteTimeRemainingMillis()
                        : game.getBlackTimeRemainingMillis();

        game.setTurnExpiresAt(
                now.plus(
                        Duration.ofMillis(
                                nextPlayerRemainingMillis
                        )
                )
        );
    }

    private void finishOnTimeout(
            Game game,
            GameState state,
            PieceColor timedOutColor
    ) {

        PieceColor opponentColor =
                timedOutColor.opposite();

        boolean opponentHasInsufficientMatingMaterial =
                gameEngine.hasInsufficientMatingMaterial(
                        state,
                        opponentColor
                );

        if (opponentHasInsufficientMatingMaterial) {

            finishGame(
                    game,
                    GameResult.DRAW,
                    GameTermination.TIMEOUT_INSUFFICIENT_MATERIAL
            );

            return;
        }

        GameResult result =
                opponentColor == PieceColor.WHITE
                        ? GameResult.WHITE_WIN
                        : GameResult.BLACK_WIN;

        finishGame(
                game,
                result,
                GameTermination.TIMEOUT
        );
    }

    private boolean hasTimedOut(
            Game game,
            GameState state,
            PieceColor movingColor,
            LocalDateTime now
    ) {

        LocalDateTime turnStartedAt =
                game.getTurnStartedAt();

        if (turnStartedAt == null) {

            throw new IllegalStateException(
                    "Turn start time is missing."
            );
        }

        long elapsedMillis =
                Duration.between(
                        turnStartedAt,
                        now
                ).toMillis();

        long remainingMillis =
                movingColor == PieceColor.WHITE
                        ? game.getWhiteTimeRemainingMillis()
                        : game.getBlackTimeRemainingMillis();

        if (remainingMillis - elapsedMillis > 0) {
            return false;
        }

        if (movingColor == PieceColor.WHITE) {

            game.setWhiteTimeRemainingMillis(
                    0L
            );

        } else {

            game.setBlackTimeRemainingMillis(
                    0L
            );
        }

        finishOnTimeout(
                game,
                state,
                movingColor
        );

        return true;
    }

    private void generateAndSetPgn(
            Game game
    ) {

        List<GameMove> moves =
                gameMoveRepository
                        .findByGameIdOrderByPlyNumberAsc(
                                game.getId()
                        );

        String pgn =
                pgnGenerator.generate(
                        moves,
                        game.getResult()
                );

        game.setPgn(
                pgn
        );
    }

    private Game executeMove(
            Game game,
            User player,
            GameState state,
            Move move,
            PieceColor movingColor,
            LocalDateTime now
    ) {

        int plyNumber =
                calculatePlyNumber(
                        state
                );

        GameState beforeState =
                state.copy();

        gameEngine.makeMove(
                state,
                move
        );

        String san =
                gameEngine.generateSan(
                        beforeState,
                        move,
                        state
                );

        updateClockAfterMove(
                game,
                movingColor,
                now
        );

        RepetitionTracker repetitionTracker =
                restoreRepetitionTracker(
                        game
                );

        String newFen =
                gameEngine.toFen(
                        state
                );

        game.setCurrentFen(
                newFen
        );

        repetitionTracker.recordPosition(
                state
        );

        clearOpponentDrawOffer(
                game,
                player
        );

        saveGameMove(
                game,
                move,
                plyNumber,
                san,
                newFen
        );

        finishIfGameEnded(
                game,
                state,
                repetitionTracker
        );

        return gameRepository.save(
                game
        );
    }

    private PieceColor resolvePlayerColor(
            Game game,
            User player
    ) {

        if (game.getWhitePlayer()
                .getId()
                .equals(
                        player.getId()
                )) {

            return PieceColor.WHITE;
        }

        if (game.getBlackPlayer()
                .getId()
                .equals(
                        player.getId()
                )) {

            return PieceColor.BLACK;
        }

        return null;
    }

    private ClockSnapshot calculateClockSnapshot(
            Game game
    ) {

        long whiteMillis =
                game.getWhiteTimeRemainingMillis();

        long blackMillis =
                game.getBlackTimeRemainingMillis();


        if (
                game.getStatus()
                        != GameStatus.IN_PROGRESS
                        || game.getTurnStartedAt() == null
        ) {

            return new ClockSnapshot(
                    whiteMillis,
                    blackMillis
            );
        }


        LocalDateTime now =
                LocalDateTime.now(
                        clock
                );


        long elapsedMillis =
                Math.max(
                        0L,
                        Duration.between(
                                game.getTurnStartedAt(),
                                now
                        ).toMillis()
                );


        GameState state =
                gameEngine.fromFen(
                        game.getCurrentFen()
                );


        if (
                state.getSideToMove()
                        == PieceColor.WHITE
        ) {

            whiteMillis =
                    Math.max(
                            0L,
                            whiteMillis
                                    - elapsedMillis
                    );

        } else {

            blackMillis =
                    Math.max(
                            0L,
                            blackMillis
                                    - elapsedMillis
                    );
        }


        return new ClockSnapshot(
                whiteMillis,
                blackMillis
        );
    }

    private void finishGameInternal(
            Game game,
            GameResult result,
            GameTermination termination,
            boolean updateTournament
    ) {

        if (game.getStatus()
                != GameStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Only an active game can be finished."
            );
        }


        if (result == null) {

            throw new IllegalArgumentException(
                    "Game result cannot be null."
            );
        }


        if (termination == null) {

            throw new IllegalArgumentException(
                    "Game termination cannot be null."
            );
        }


        game.setResult(
                result
        );

        game.setTermination(
                termination
        );

        game.setStatus(
                GameStatus.FINISHED
        );

        game.setFinishedAt(
                LocalDateTime.now(
                        clock
                )
        );


        game.setTurnExpiresAt(
                null
        );


        generateAndSetPgn(
                game
        );


        ratingService.updateRatings(
                game
        );


        if (updateTournament
                && game.getTournamentMatch() != null) {

            Tournament tournament =
                    game.getTournamentMatch()
                            .getTournament();


            tournamentService
                    .recordGameResult(
                            game
                    );


            startGamesForCurrentRound(
                    tournament
            );
        }


        game.setDrawOfferBy(
                null
        );
    }
}