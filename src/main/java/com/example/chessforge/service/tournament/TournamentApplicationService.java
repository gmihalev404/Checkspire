package com.example.chessforge.service.tournament;

import com.example.chessforge.controller.tournament.dto.TournamentCreateRequest;
import com.example.chessforge.model.entity.tournament.Tournament;
import com.example.chessforge.model.entity.tournament.TournamentParticipant;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.tournament.TournamentParticipantStatus;
import com.example.chessforge.service.game.RatingService;
import com.example.chessforge.service.tournament.dto.TournamentDetailsResponse;
import com.example.chessforge.service.tournament.dto.TournamentParticipantResponse;
import com.example.chessforge.service.tournament.dto.TournamentSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentApplicationService {

    private final TournamentService tournamentService;
    private final RatingService ratingService;


    public List<TournamentSummaryResponse> getTournaments() {

        return tournamentService
                .getAllTournaments()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Tournament::getUpdatedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .map(
                        this::toSummary
                )
                .toList();
    }

    @Transactional
    public Long createTournament(
            User creator,
            TournamentCreateRequest request
    ) {

        Tournament tournament =
                tournamentService.createTournament(
                        request.getName(),
                        creator,
                        request.getFormat(),
                        request.getTimeControl(),
                        request.isRated(),
                        request.getMaxPlayers(),
                        request.getStartsAt(),
                        request.getByePoints(),
                        request.getTieBreaks(),
                        request.isArmageddonForFirstPlaceTie(),
                        request.getNumberOfRounds()
                );

        return tournament.getId();
    }

    private TournamentSummaryResponse toSummary(
            Tournament tournament
    ) {

        return new TournamentSummaryResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getCreator()
                        .getUsername(),
                tournament.getFormat(),
                tournament.getTimeControl(),
                tournament.isRated(),
                tournament.getStatus(),
                tournament.getMaxPlayers(),
                tournament.getStartsAt(),
                tournament.getUpdatedAt()
        );
    }

    public TournamentDetailsResponse getTournamentDetails(
            Long tournamentId,
            User viewer
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );


        List<TournamentParticipant> participants =
                tournamentService
                        .getParticipants(
                                tournament
                        );


        List<TournamentParticipantResponse> participantResponses =
                participants
                        .stream()
                        .filter(participant ->
                                participant.getStatus()
                                        != TournamentParticipantStatus.WITHDRAWN
                        )
                        .map(participant -> {

                            int rating =
                                    participant.getRatingAtStart() != null
                                            ? participant.getRatingAtStart()
                                            : ratingService.getRating(
                                            participant.getUser(),
                                            tournament
                                                    .getTimeControl()
                                                    .getType()
                                    );

                            return new TournamentParticipantResponse(
                                    participant
                                            .getUser()
                                            .getUsername(),
                                    rating,
                                    participant.getScore(),
                                    participant.getSeed(),
                                    participant.getStatus()
                            );
                        })
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                TournamentParticipantResponse::rating
                                        )
                                        .reversed()
                                        .thenComparing(
                                                TournamentParticipantResponse::username,
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                        )
                        .toList();


        boolean viewerJoined =
                participants
                        .stream()
                        .anyMatch(participant ->
                                participant
                                        .getUser()
                                        .getId()
                                        .equals(
                                                viewer.getId()
                                        )
                                        &&
                                        participant.getStatus()
                                                == TournamentParticipantStatus.ACTIVE
                        );


        boolean viewerCreator =
                tournament
                        .getCreator()
                        .getId()
                        .equals(
                                viewer.getId()
                        );


        return new TournamentDetailsResponse(
                tournament.getId(),
                tournament.getName(),
                tournament
                        .getCreator()
                        .getUsername(),
                tournament.getFormat(),
                tournament.getTimeControl(),
                tournament.isRated(),
                tournament.getStatus(),
                tournament.getMaxPlayers(),
                tournament.getStartsAt(),
                tournament.getByePoints(),
                tournament.getTieBreaks(),
                tournament.isArmageddonForFirstPlaceTie(),
                tournament.getNumberOfRounds(),
                tournament.getCurrentRound(),
                participantResponses,
                viewerJoined,
                viewerCreator
        );
    }


    @Transactional
    public void joinTournament(
            Long tournamentId,
            User user
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );

        tournamentService.joinTournament(
                tournament,
                user
        );
    }


    @Transactional
    public void withdrawFromTournament(
            Long tournamentId,
            User user
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );

        tournamentService.withdraw(
                tournament,
                user
        );
    }

    @Transactional
    public void startTournament(
            Long tournamentId,
            User requester
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );

        tournamentService.startTournament(
                tournament,
                requester
        );
    }


    @Transactional
    public void cancelTournament(
            Long tournamentId,
            User requester
    ) {

        Tournament tournament =
                tournamentService.getTournament(
                        tournamentId
                );

        tournamentService.cancelTournament(
                tournament,
                requester
        );
    }
}