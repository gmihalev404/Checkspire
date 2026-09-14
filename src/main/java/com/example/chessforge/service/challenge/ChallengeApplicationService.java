package com.example.chessforge.service.challenge;

import com.example.chessforge.model.entity.challenge.Challenge;
import com.example.chessforge.model.entity.game.Game;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.challenge.ColorPreference;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import com.example.chessforge.repository.challenge.ChallengeRepository;
import com.example.chessforge.service.challenge.dto.ChallengePageResponse;
import com.example.chessforge.service.challenge.dto.ChallengeSummaryResponse;
import com.example.chessforge.service.game.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeApplicationService {

    private final ChallengeService challengeService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeRealtimePublisher challengeRealtimePublisher;
    private final GameService gameService;



    public ChallengePageResponse getChallenges(
            User user
    ) {

        List<ChallengeSummaryResponse> incoming =
                challengeService
                        .getIncomingPendingChallenges(
                                user
                        )
                        .stream()
                        .map(challenge ->
                                toSummary(
                                        challenge,
                                        challenge.getChallenger()
                                )
                        )
                        .toList();


        List<ChallengeSummaryResponse> outgoing =
                challengeService
                        .getOutgoingPendingChallenges(
                                user
                        )
                        .stream()
                        .map(challenge ->
                                toSummary(
                                        challenge,
                                        challenge.getOpponent()
                                )
                        )
                        .toList();


        return new ChallengePageResponse(
                incoming,
                outgoing
        );
    }


    @Transactional
    public void sendChallenge(
            User challenger,
            User opponent,
            TimeControl timeControl,
            ColorPreference colorPreference,
            boolean rated
    ) {

        challengeService.sendChallenge(
                challenger,
                opponent,
                timeControl,
                colorPreference,
                rated
        );
    }


    @Transactional
    public Long acceptAndStart(
            Long challengeId,
            User opponent
    ) {

        Challenge challenge =
                getChallenge(
                        challengeId
                );


        challengeService.acceptChallenge(
                challenge,
                opponent
        );


        Game game =
                gameService.createGameFromChallenge(
                        challenge
                );


        gameService.startGame(
                game
        );

        challengeRealtimePublisher
                .publishGameStarted(
                        challenge.getChallenger(),
                        challenge.getOpponent(),
                        game.getId()
                );

        return game.getId();
    }


    @Transactional
    public void decline(
            Long challengeId,
            User opponent
    ) {

        Challenge challenge =
                getChallenge(
                        challengeId
                );

        challengeService.declineChallenge(
                challenge,
                opponent
        );
    }


    @Transactional
    public void cancel(
            Long challengeId,
            User challenger
    ) {

        Challenge challenge =
                getChallenge(
                        challengeId
                );

        challengeService.cancelChallenge(
                challenge,
                challenger
        );
    }


    private Challenge getChallenge(
            Long challengeId
    ) {

        return challengeRepository
                .findById(
                        challengeId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Challenge not found."
                        )
                );
    }


    private ChallengeSummaryResponse toSummary(
            Challenge challenge,
            User otherUser
    ) {

        return new ChallengeSummaryResponse(
                challenge.getId(),
                otherUser.getUsername(),
                challenge.getTimeControl(),
                challenge.getColorPreference(),
                challenge.isRated(),
                challenge.getExpiresAt()
        );
    }
}