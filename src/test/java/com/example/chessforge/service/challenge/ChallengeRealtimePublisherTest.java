package com.example.chessforge.service.challenge;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.challenge.dto.GameStartedMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeRealtimePublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChallengeRealtimePublisher
            challengeRealtimePublisher;

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


    @AfterEach
    void tearDown() {

        if (
                TransactionSynchronizationManager
                        .isSynchronizationActive()
        ) {
            TransactionSynchronizationManager
                    .clearSynchronization();
        }
    }


    @Test
    void publishGameStartedShouldPublishImmediatelyWhenNoTransactionExists() {

        Long gameId = 15L;


        challengeRealtimePublisher
                .publishGameStarted(
                        challenger,
                        opponent,
                        gameId
                );


        verify(messagingTemplate)
                .convertAndSendToUser(
                        eq("challenger"),
                        eq("/queue/game-started"),
                        argThat(message ->
                                message instanceof GameStartedMessage
                                        && ((GameStartedMessage) message)
                                        .gameId()
                                        .equals(gameId)
                        )
                );

        verify(messagingTemplate)
                .convertAndSendToUser(
                        eq("opponent"),
                        eq("/queue/game-started"),
                        argThat(message ->
                                message instanceof GameStartedMessage
                                        && ((GameStartedMessage) message)
                                        .gameId()
                                        .equals(gameId)
                        )
                );
    }


    @Test
    void publishGameStartedShouldWaitForCommitWhenTransactionSynchronizationExists() {

        Long gameId = 16L;

        TransactionSynchronizationManager
                .initSynchronization();


        challengeRealtimePublisher
                .publishGameStarted(
                        challenger,
                        opponent,
                        gameId
                );


        verifyNoInteractions(
                messagingTemplate
        );


        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager
                        .getSynchronizations();

        for (
                TransactionSynchronization synchronization
                : synchronizations
        ) {
            synchronization.afterCommit();
        }


        verify(messagingTemplate)
                .convertAndSendToUser(
                        eq("challenger"),
                        eq("/queue/game-started"),
                        argThat(message ->
                                message instanceof GameStartedMessage
                                        && ((GameStartedMessage) message)
                                        .gameId()
                                        .equals(gameId)
                        )
                );

        verify(messagingTemplate)
                .convertAndSendToUser(
                        eq("opponent"),
                        eq("/queue/game-started"),
                        argThat(message ->
                                message instanceof GameStartedMessage
                                        && ((GameStartedMessage) message)
                                        .gameId()
                                        .equals(gameId)
                        )
                );
    }


    @Test
    void publishGameStartedShouldNotifyBothPlayersExactlyOnce() {

        Long gameId = 17L;


        challengeRealtimePublisher
                .publishGameStarted(
                        challenger,
                        opponent,
                        gameId
                );


        verify(
                messagingTemplate,
                times(1)
        ).convertAndSendToUser(
                eq("challenger"),
                eq("/queue/game-started"),
                any(GameStartedMessage.class)
        );

        verify(
                messagingTemplate,
                times(1)
        ).convertAndSendToUser(
                eq("opponent"),
                eq("/queue/game-started"),
                any(GameStartedMessage.class)
        );

        verifyNoMoreInteractions(
                messagingTemplate
        );
    }
}