package com.example.checkspire.service.game;

import com.example.checkspire.model.entity.game.Game;
import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.service.game.GameService;
import com.example.checkspire.service.game.GameStateMapper;
import com.example.checkspire.service.game.dto.GameStateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameActionApplicationService {

    private final GameService gameService;
    private final GameStateMapper gameStateMapper;

    @Transactional
    public GameStateResponse abortAndGetState(
            Long gameId,
            User player
    ) {

        Game game =
                gameService.abortGame(
                        gameId,
                        player
                );

        return gameStateMapper.toResponse(
                game
        );
    }


    @Transactional
    public GameStateResponse resignAndGetState(
            Long gameId,
            User player
    ) {

        Game game =
                gameService.resignGame(
                        gameId,
                        player
                );

        return gameStateMapper.toResponse(
                game
        );
    }


    @Transactional
    public GameStateResponse offerDrawAndGetState(
            Long gameId,
            User player
    ) {

        Game game =
                gameService.offerDraw(
                        gameId,
                        player
                );

        return gameStateMapper.toResponse(
                game
        );
    }


    @Transactional
    public GameStateResponse acceptDrawAndGetState(
            Long gameId,
            User player
    ) {

        Game game =
                gameService.acceptDraw(
                        gameId,
                        player
                );

        return gameStateMapper.toResponse(
                game
        );
    }


    @Transactional
    public GameStateResponse rejectDrawAndGetState(
            Long gameId,
            User player
    ) {

        Game game =
                gameService.rejectDraw(
                        gameId,
                        player
                );

        return gameStateMapper.toResponse(
                game
        );
    }
}