package com.example.checkspire.scheduler.tournament;

import com.example.checkspire.model.entity.tournament.Tournament;
import com.example.checkspire.model.enums.tournament.TournamentStatus;
import com.example.checkspire.repository.tournament.TournamentRepository;
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
public class TournamentStartScheduler {

    private final TournamentRepository tournamentRepository;

    private final TournamentApplicationService
            tournamentApplicationService;

    private final Clock clock;


    @Scheduled(
            fixedDelayString =
                    "${Checkspire.tournament.start-check-delay-ms:1000}"
    )
    public void startDueTournaments() {

        LocalDateTime now =
                LocalDateTime.now(
                        clock
                );


        List<Tournament> dueTournaments =
                tournamentRepository
                        .findByStatusAndAutomaticStartTrueAndStartsAtLessThanEqualOrderByStartsAtAsc(
                                TournamentStatus.REGISTRATION,
                                now
                        );


        for (Tournament tournament : dueTournaments) {

            try {

                tournamentApplicationService
                        .startTournamentAutomatically(
                                tournament.getId()
                        );

            } catch (
                    IllegalArgumentException
                    | IllegalStateException exception
            ) {

                log.warn(
                        "Could not automatically start tournament {}: {}",
                        tournament.getId(),
                        exception.getMessage()
                );
            }
        }
    }
}