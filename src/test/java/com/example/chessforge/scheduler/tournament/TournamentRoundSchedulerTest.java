package com.example.chessforge.scheduler.tournament;

import com.example.chessforge.model.entity.tournament.TournamentRound;
import com.example.chessforge.model.enums.tournament.TournamentRoundStatus;
import com.example.chessforge.repository.tournament.TournamentRoundRepository;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentRoundSchedulerTest {

    @Mock
    private TournamentRoundRepository roundRepository;

    @Mock
    private TournamentApplicationService
            tournamentApplicationService;

    @Spy
    private Clock clock =
            Clock.systemDefaultZone();

    @InjectMocks
    private TournamentRoundScheduler scheduler;


    @Test
    void shouldActivateDueRounds() {

        mockCurrentTime(
                "2026-09-15T12:00:00Z"
        );


        TournamentRound first =
                mock(
                        TournamentRound.class
                );

        TournamentRound second =
                mock(
                        TournamentRound.class
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
                roundRepository
                        .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                                eq(TournamentRoundStatus.SCHEDULED),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(
                List.of(
                        first,
                        second
                )
        );


        scheduler.startDueRounds();


        verify(
                tournamentApplicationService
        ).activateScheduledRound(
                10L
        );

        verify(
                tournamentApplicationService
        ).activateScheduledRound(
                20L
        );
    }


    @Test
    void shouldDoNothingWhenThereAreNoDueRounds() {

        mockCurrentTime(
                "2026-09-15T12:00:00Z"
        );


        when(
                roundRepository
                        .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                                eq(TournamentRoundStatus.SCHEDULED),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(
                List.of()
        );


        scheduler.startDueRounds();


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