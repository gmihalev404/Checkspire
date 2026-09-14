package com.example.chessforge.service.challenge;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.challenge.dto.GameStartedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ChallengeRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;


    public void publishGameStarted(
            User challenger,
            User opponent,
            Long gameId
    ) {

        System.out.println(
                "Challenger target username: "
                        + challenger.getUsername()
        );

        System.out.println(
                "Opponent target username: "
                        + opponent.getUsername()
        );

        System.out.println(
                "Connected WebSocket users:"
        );

        for (
                SimpUser user
                : simpUserRegistry.getUsers()
        ) {

            System.out.println(
                    " - "
                            + user.getName()
            );
        }

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

        System.out.println(
                "Sending game-started to: "
                        + user.getUsername()
        );

        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/game-started",
                new GameStartedMessage(
                        gameId
                )
        );
    }
}