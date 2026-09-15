package com.example.chessforge.scheduler.tournament;

import com.example.chessforge.model.entity.tournament.TournamentRound;
import com.example.chessforge.model.enums.tournament.TournamentRoundStatus;
import com.example.chessforge.repository.tournament.TournamentRoundRepository;
import com.example.chessforge.service.tournament.TournamentApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TournamentRoundScheduler {

    private final TournamentRoundRepository roundRepository;

    private final TournamentApplicationService
            tournamentApplicationService;

    private final Clock clock;


    @Scheduled(
            fixedDelayString =
                    "${chessforge.tournament.round-check-delay-ms:1000}"
    )
    public void startDueRounds() {

        LocalDateTime now =
                LocalDateTime.now(
                        clock
                );


        List<TournamentRound> dueRounds =
                roundRepository
                        .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                                TournamentRoundStatus.SCHEDULED,
                                now
                        );


        for (TournamentRound round : dueRounds) {

            try {

                tournamentApplicationService
                        .activateScheduledRound(
                                round.getId()
                        );

            } catch (
                    IllegalArgumentException
                    | IllegalStateException exception
            ) {

                log.warn(
                        "Could not activate tournament round {}: {}",
                        round.getId(),
                        exception.getMessage()
                );
            }
        }
    }
}