package com.example.checkspire.scheduler.tournament;

import com.example.checkspire.model.entity.tournament.TournamentRound;
import com.example.checkspire.model.enums.tournament.TournamentRoundStatus;
import com.example.checkspire.repository.tournament.TournamentRoundRepository;
import com.example.checkspire.service.tournament.TournamentApplicationService;
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
                    "${Checkspire.tournament.round-check-delay-ms:1000}"
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