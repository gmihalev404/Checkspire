package com.example.checkspire.repository.tournament;

import com.example.checkspire.model.entity.tournament.Tournament;
import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.model.enums.tournament.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TournamentRepository
        extends JpaRepository<Tournament, Long> {

    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByCreator(User creator);

    List<Tournament>
    findByStatusAndAutomaticStartTrueAndStartsAtLessThanEqualOrderByStartsAtAsc(
            TournamentStatus status,
            LocalDateTime startsAt
    );

}