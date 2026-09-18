package com.example.checkspire.repository.tournament;

import com.example.checkspire.model.entity.tournament.Tournament;
import com.example.checkspire.model.entity.tournament.TournamentRound;
import com.example.checkspire.model.enums.tournament.TournamentRoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TournamentRoundRepository
        extends JpaRepository<TournamentRound, Long> {

    List<TournamentRound>
    findByTournamentOrderByRoundNumberAsc(
            Tournament tournament
    );

    Optional<TournamentRound>
    findByTournamentAndRoundNumber(
            Tournament tournament,
            Integer roundNumber
    );

    List<TournamentRound>
    findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            TournamentRoundStatus status,
            LocalDateTime scheduledAt
    );
}