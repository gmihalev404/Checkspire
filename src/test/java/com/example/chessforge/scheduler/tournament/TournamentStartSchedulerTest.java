package com.example.chessforge.scheduler.tournament;

import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.enums.tournament.TournamentStatus;
import com.example.chessforge.repository.tournament.TournamentRepository;
import com.example.chessforge.service.tournament.TournamentApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentStartSchedulerTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentApplicationService
            tournamentApplicationService;

    @Spy
    private Clock clock =
            Clock.systemDefaultZone();

    @InjectMocks
    private TournamentStartScheduler scheduler;


    @Test
    void shouldStartDueAutomaticTournaments() {

        mockCurrentTime(
                "2026-09-15T12:00:00Z"
        );


        Tournament first =
                mock(
                        Tournament.class
                );

        Tournament second =
                mock(
                        Tournament.class
                );


        when(first.getId())
                .thenReturn(
                        10L
                );

        when(second.getId())
                .thenReturn(
                        20L
                );


        when(
                tournamentRepository
                        .findByStatusAndAutomaticStartTrueAndStartsAtLessThanEqualOrderByStartsAtAsc(
                                eq(TournamentStatus.REGISTRATION),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(
                List.of(
                        first,
                        second
                )
        );


        scheduler.startDueTournaments();


        verify(
                tournamentApplicationService
        ).startTournamentAutomatically(
                10L
        );

        verify(
                tournamentApplicationService
        ).startTournamentAutomatically(
                20L
        );
    }


    @Test
    void shouldDoNothingWhenThereAreNoDueTournaments() {

        mockCurrentTime(
                "2026-09-15T12:00:00Z"
        );


        when(
                tournamentRepository
                        .findByStatusAndAutomaticStartTrueAndStartsAtLessThanEqualOrderByStartsAtAsc(
                                eq(TournamentStatus.REGISTRATION),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(
                List.of()
        );


        scheduler.startDueTournaments();


        verifyNoInteractions(
                tournamentApplicationService
        );
    }


    private void mockCurrentTime(
            String instant
    ) {

        doReturn(
                ZoneId.of("UTC")
        ).when(clock)
                .getZone();

        doReturn(
                Instant.parse(instant)
        ).when(clock)
                .instant();
    }
}