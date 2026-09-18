package com.example.checkspire.service.challenge;

import com.example.checkspire.model.entity.challenge.Challenge;
import com.example.checkspire.model.entity.game.Game;
import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.model.enums.challenge.ColorPreference;
import com.example.checkspire.model.enums.timeControl.TimeControl;
import com.example.checkspire.repository.challenge.ChallengeRepository;
import com.example.checkspire.service.challenge.dto.ChallengePageResponse;
import com.example.checkspire.service.game.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeApplicationServiceTest {

    @Mock
    private ChallengeService challengeService;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ChallengeRealtimePublisher challengeRealtimePublisher;

    @Mock
    private GameService gameService;

    @InjectMocks
    private ChallengeApplicationService
            challengeApplicationService;

    private User challenger;
    private User opponent;


    @BeforeEach
    void setUp() {

        challenger =
                User.builder()
                        .username("challenger")
                        .build();

        opponent =
                User.builder()
                        .username("opponent")
                        .build();
    }


    @Test
    void getChallengesShouldReturnIncomingAndOutgoingChallenges() {

        Challenge incoming =
                mock(
                        Challenge.class
                );

        Challenge outgoing =
                mock(
                        Challenge.class
                );


        when(
                challengeService
                        .getIncomingPendingChallenges(
                                challenger
                        )
        ).thenReturn(
                List.of(
                        incoming
                )
        );

        when(
                challengeService
                        .getOutgoingPendingChallenges(
                                challenger
                        )
        ).thenReturn(
                List.of(
                        outgoing
                )
        );


        when(
                incoming.getChallenger()
        ).thenReturn(
                opponent
        );

        when(
                incoming.getId()
        ).thenReturn(
                1L
        );

        when(
                incoming.getTimeControl()
        ).thenReturn(
                TimeControl.RAPID_10_0
        );

        when(
                incoming.getColorPreference()
        ).thenReturn(
                ColorPreference.WHITE
        );

        when(
                incoming.isRated()
        ).thenReturn(
                true
        );

        when(
                incoming.getExpiresAt()
        ).thenReturn(
                LocalDateTime.of(
                        2026,
                        9,
                        14,
                        20,
                        0
                )
        );


        when(
                outgoing.getOpponent()
        ).thenReturn(
                opponent
        );

        when(
                outgoing.getId()
        ).thenReturn(
                2L
        );

        when(
                outgoing.getTimeControl()
        ).thenReturn(
                TimeControl.RAPID_10_0
        );

        when(
                outgoing.getColorPreference()
        ).thenReturn(
                ColorPreference.BLACK
        );

        when(
                outgoing.isRated()
        ).thenReturn(
                false
        );

        when(
                outgoing.getExpiresAt()
        ).thenReturn(
                LocalDateTime.of(
                        2026,
                        9,
                        14,
                        21,
                        0
                )
        );


        ChallengePageResponse result =
                challengeApplicationService
                        .getChallenges(
                                challenger
                        );


        assertEquals(
                1,
                result.incoming()
                        .size()
        );

        assertEquals(
                1,
                result.outgoing()
                        .size()
        );

        verify(challengeService)
                .getIncomingPendingChallenges(
                        challenger
                );

        verify(challengeService)
                .getOutgoingPendingChallenges(
                        challenger
                );
    }


    @Test
    void sendChallengeShouldDelegateToChallengeService() {

        challengeApplicationService
                .sendChallenge(
                        challenger,
                        opponent,
                        TimeControl.RAPID_10_0,
                        ColorPreference.RANDOM,
                        true
                );


        verify(challengeService)
                .sendChallenge(
                        challenger,
                        opponent,
                        TimeControl.RAPID_10_0,
                        ColorPreference.RANDOM,
                        true
                );
    }


    @Test
    void acceptAndStartShouldAcceptCreateStartAndPublishGame() {

        Long challengeId = 1L;
        Long gameId = 25L;

        Challenge challenge =
                mock(
                        Challenge.class
                );

        Game game =
                mock(
                        Game.class
                );


        when(
                challengeRepository.findById(
                        challengeId
                )
        ).thenReturn(
                Optional.of(
                        challenge
                )
        );

        when(
                challenge.getChallenger()
        ).thenReturn(
                challenger
        );

        when(
                challenge.getOpponent()
        ).thenReturn(
                opponent
        );

        when(
                gameService
                        .createGameFromChallenge(
                                challenge
                        )
        ).thenReturn(
                game
        );

        when(
                game.getId()
        ).thenReturn(
                gameId
        );


        Long result =
                challengeApplicationService
                        .acceptAndStart(
                                challengeId,
                                opponent
                        );


        assertEquals(
                gameId,
                result
        );


        InOrder inOrder =
                inOrder(
                        challengeService,
                        gameService,
                        challengeRealtimePublisher
                );


        inOrder.verify(
                challengeService
        ).acceptChallenge(
                challenge,
                opponent
        );

        inOrder.verify(
                gameService
        ).createGameFromChallenge(
                challenge
        );

        inOrder.verify(
                gameService
        ).startGame(
                game
        );

        inOrder.verify(
                challengeRealtimePublisher
        ).publishGameStarted(
                challenger,
                opponent,
                gameId
        );
    }


    @Test
    void declineShouldLoadChallengeAndDelegate() {

        Long challengeId = 1L;

        Challenge challenge =
                mock(
                        Challenge.class
                );


        when(
                challengeRepository.findById(
                        challengeId
                )
        ).thenReturn(
                Optional.of(
                        challenge
                )
        );


        challengeApplicationService
                .decline(
                        challengeId,
                        opponent
                );


        verify(challengeService)
                .declineChallenge(
                        challenge,
                        opponent
                );
    }


    @Test
    void cancelShouldLoadChallengeAndDelegate() {

        Long challengeId = 1L;

        Challenge challenge =
                mock(
                        Challenge.class
                );


        when(
                challengeRepository.findById(
                        challengeId
                )
        ).thenReturn(
                Optional.of(
                        challenge
                )
        );


        challengeApplicationService
                .cancel(
                        challengeId,
                        challenger
                );


        verify(challengeService)
                .cancelChallenge(
                        challenge,
                        challenger
                );
    }


    @Test
    void acceptAndStartShouldThrowWhenChallengeDoesNotExist() {

        Long challengeId = 999L;

        when(
                challengeRepository.findById(
                        challengeId
                )
        ).thenReturn(
                Optional.empty()
        );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                challengeApplicationService
                                        .acceptAndStart(
                                                challengeId,
                                                opponent
                                        )
                );


        assertEquals(
                "Challenge not found.",
                exception.getMessage()
        );

        verifyNoInteractions(
                challengeService,
                gameService,
                challengeRealtimePublisher
        );
    }
}