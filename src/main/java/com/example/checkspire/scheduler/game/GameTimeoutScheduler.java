package com.example.checkspire.scheduler.game;

import com.example.checkspire.model.enums.game.GameStatus;
import com.example.checkspire.repository.game.GameRepository;
import com.example.checkspire.service.game.GameService;
import com.example.checkspire.service.game.realtime.GameRealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GameTimeoutScheduler {

    private static final int BATCH_SIZE =
            100;

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final Clock clock;
    private final GameRealtimePublisher realtimePublisher;

    @Scheduled(
            fixedDelayString =
                    "${Checkspire.game.timeout-check-delay-ms:1000}"
    )
    public void finalizeExpiredGames() {

        LocalDateTime now =
                LocalDateTime.now(
                        clock
                );

        List<Long> expiredGameIds =
                gameRepository.findExpiredGameIds(
                        GameStatus.IN_PROGRESS,
                        now,
                        PageRequest.of(
                                0,
                                BATCH_SIZE
                        )
                );

        for (Long gameId : expiredGameIds) {

            gameService
                    .finalizeTimeoutIfExpiredAndGetState(
                            gameId
                    )
                    .ifPresent(response ->
                            realtimePublisher.publish(
                                    gameId,
                                    response
                            )
                    );
        }
    }
}