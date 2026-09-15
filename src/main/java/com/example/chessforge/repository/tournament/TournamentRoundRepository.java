package com.example.chessforge.repository.tournament;

import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.tournament.TournamentRound;
import org.springframework.data.jpa.repository.JpaRepository;

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
}