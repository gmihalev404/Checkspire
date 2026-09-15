package com.example.chessforge.service.tournament;

import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}