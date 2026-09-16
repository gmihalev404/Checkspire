package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.common.BaseEntity;
import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.tournament.TournamentMatch;
import com.example.chessforge.model.entity.tournament.TournamentParticipant;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.game.GameStatus;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentApplicationServiceTest {

    @Mock
    private TournamentService tournamentService;

    @Mock
    private RatingService ratingService;

    @Mock
    private GameService gameService;

    @InjectMocks
    private TournamentApplicationService
            tournamentApplicationService;


    @Test
    void startTournamentAutomaticallyShouldStartGamesForFirstRound() {

        Long tournamentId =
                10L;

        Tournament tournament =
                mock(
                        Tournament.class
                );


        when(
                tournamentService
                        .startTournamentAutomatically(
                                tournamentId
                        )
        ).thenReturn(
                tournament
        );


        tournamentApplicationService
                .startTournamentAutomatically(
                        tournamentId
                );


        verify(
                tournamentService
        ).startTournamentAutomatically(
                tournamentId
        );

        verify(
                gameService
        ).startGamesForCurrentRound(
                tournament
        );
    }


    @Test
    void activateScheduledRoundShouldStartGamesForActivatedRound() {

        Long roundId =
                20L;

        Tournament tournament =
                mock(
                        Tournament.class
                );


        when(
                tournamentService
                        .activateScheduledRound(
                                roundId
                        )
        ).thenReturn(
                tournament
        );


        tournamentApplicationService
                .activateScheduledRound(
                        roundId
                );


        verify(
                tournamentService
        ).activateScheduledRound(
                roundId
        );

        verify(
                gameService
        ).startGamesForCurrentRound(
                tournament
        );
    }

    @Test
    void startScheduledRoundNowShouldActivateRoundAndStartGames() {

        Long tournamentId = 10L;
        Integer roundNumber = 2;

        User creator =
                mock(
                        User.class
                );

        Tournament tournament =
                mock(
                        Tournament.class
                );


        when(
                tournamentService.getTournament(
                        tournamentId
                )
        ).thenReturn(
                tournament
        );

        when(
                tournamentService
                        .startScheduledRoundNow(
                                tournament,
                                roundNumber,
                                creator
                        )
        ).thenReturn(
                tournament
        );


        tournamentApplicationService
                .startScheduledRoundNow(
                        tournamentId,
                        roundNumber,
                        creator
                );


        verify(
                tournamentService
        ).startScheduledRoundNow(
                tournament,
                roundNumber,
                creator
        );

        verify(
                gameService
        ).startGamesForCurrentRound(
                tournament
        );
    }

    @Test
    void forfeitTournamentShouldWorkWithoutActiveGame() {

        Long tournamentId = 10L;

        User user =
                mock(
                        User.class
                );

        Tournament tournament =
                mock(
                        Tournament.class
                );


        when(
                tournamentService.getTournament(
                        tournamentId
                )
        ).thenReturn(
                tournament
        );

        when(
                tournament.getCurrentRound()
        ).thenReturn(
                1
        );

        when(
                tournamentService.getRoundMatches(
                        tournament,
                        1
                )
        ).thenReturn(
                List.of()
        );


        tournamentApplicationService
                .forfeitTournament(
                        tournamentId,
                        user
                );


        verify(
                tournamentService
        ).forfeit(
                tournament,
                user
        );

        verify(
                gameService,
                never()
        ).forfeitTournamentGame(
                anyLong(),
                any()
        );
    }

    @Test
    void forfeitTournamentShouldAlsoFinishActiveTournamentGame() {

        Long tournamentId = 10L;
        Long gameId = 50L;


        User user =
                mock(
                        User.class
                );

        User opponent =
                mock(
                        User.class
                );


        when(
                user.getId()
        ).thenReturn(
                1L
        );

        when(
                opponent.getId()
        ).thenReturn(
                2L
        );


        Tournament tournament =
                mock(
                        Tournament.class
                );

        when(
                tournament.getCurrentRound()
        ).thenReturn(
                1
        );


        TournamentParticipant white =
                TournamentParticipant.builder()
                        .user(
                                user
                        )
                        .build();

        TournamentParticipant black =
                TournamentParticipant.builder()
                        .user(
                                opponent
                        )
                        .build();


        TournamentMatch match =
                TournamentMatch.builder()
                        .tournament(
                                tournament
                        )
                        .whiteParticipant(
                                white
                        )
                        .blackParticipant(
                                black
                        )
                        .roundNumber(
                                1
                        )
                        .build();


        Game game =
                Game.builder()
                        .whitePlayer(
                                user
                        )
                        .blackPlayer(
                                opponent
                        )
                        .status(
                                GameStatus.IN_PROGRESS
                        )
                        .tournamentMatch(
                                match
                        )
                        .build();

        setId(
                game,
                gameId
        );


        when(
                tournamentService.getTournament(
                        tournamentId
                )
        ).thenReturn(
                tournament
        );

        when(
                tournamentService.getRoundMatches(
                        tournament,
                        1
                )
        ).thenReturn(
                List.of(match)
        );

        when(
                gameService
                        .getLatestGameForTournamentMatch(
                                match
                        )
        ).thenReturn(
                Optional.of(game)
        );


        tournamentApplicationService
                .forfeitTournament(
                        tournamentId,
                        user
                );


        verify(
                tournamentService
        ).forfeit(
                tournament,
                user
        );

        verify(
                gameService
        ).forfeitTournamentGame(
                gameId,
                user
        );
    }

    private void setId(
            BaseEntity entity,
            Long id
    ) {

        try {

            var field =
                    BaseEntity.class
                            .getDeclaredField(
                                    "id"
                            );

            field.setAccessible(
                    true
            );

            field.set(
                    entity,
                    id
            );

        } catch (
                ReflectiveOperationException exception
        ) {

            throw new RuntimeException(
                    exception
            );
        }
    }
}