package com.example.checkspire.service.challenge;

import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.service.challenge.dto.GameStartedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ChallengeRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;


    public void publishGameStarted(
            User challenger,
            User opponent,
            Long gameId
    ) {

        Runnable publish =
                () -> {
                    sendGameStarted(
                            challenger,
                            gameId
                    );

                    sendGameStarted(
                            opponent,
                            gameId
                    );
                };


        if (
                TransactionSynchronizationManager
                        .isSynchronizationActive()
        ) {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {

                                @Override
                                public void afterCommit() {

                                    publish.run();
                                }
                            }
                    );

            return;
        }


        publish.run();
    }


    private void sendGameStarted(
            User user,
            Long gameId
    ) {

        messagingTemplate
                .convertAndSendToUser(
                        user.getUsername(),
                        "/queue/game-started",
                        new GameStartedMessage(
                                gameId
                        )
                );
    }
}